import { DatePipe } from '@angular/common';
import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { AuthStateService } from '../../../../../core/services/auth-state.service';
import { ApprovalApiService } from '../../../infrastructure/approval-api.service';
import { LimitApplicationDetail } from '../../../domain/entities/limit-application';

type ActionType = 'APPROVE' | 'REJECT';

const BM_MAX_INCREASE = 2_000_000;

@Component({
  selector: 'app-approval-detail',
  imports: [DatePipe, FormsModule],
  templateUrl: './approval-detail.html',
  styleUrl: './approval-detail.css'
})
export class ApprovalDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly approvalApi = inject(ApprovalApiService);
  private readonly authState = inject(AuthStateService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly applicationId = Number(this.route.snapshot.paramMap.get('id'));

  private readonly userProfile = toSignal(this.authState.userProfile$, { initialValue: null });
  protected readonly isBm = () => this.userProfile()?.role?.toUpperCase() === 'BM';

  protected readonly detail = signal<LimitApplicationDetail | null>(null);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal('');

  /** object URL hasil blob fetch (lihat loadPhotos) - null selama masih dimuat/gagal/belum
   * diupload nasabah. Di-revoke di destroy biar gak bocor memory. */
  protected readonly ktpPhotoUrl = signal<string | null>(null);
  protected readonly kycPhotoUrl = signal<string | null>(null);
  protected readonly photoError = signal('');
  private readonly objectUrls: string[] = [];

  protected readonly formOpen = signal(false);
  protected readonly actionType = signal<ActionType | null>(null);
  protected readonly limitInput = signal(0);
  protected readonly reasonInput = signal('');
  protected readonly submitting = signal(false);
  protected readonly actionError = signal('');

  /** Engine merekomendasikan REJECTED (Vida gagal) -> tema merah di seluruh kartu Engine Scoring. */
  protected readonly isNegative = computed(() => this.detail()?.engineRecommendation === 'REJECTED');

  /** BM cuma boleh mutusin selagi masih PENDING_BM; Checker selagi masih PENDING_CHECKER. */
  protected readonly canDecideStatus = computed(() => {
    const d = this.detail();
    if (!d) return false;
    return this.isBm() ? d.status === 'PENDING_BM' : d.status === 'PENDING_CHECKER';
  });

  /** Cap kenaikan BM (bm-approval-lock.md/lending-flow.md): plafond Checker + Rp 2.000.000, turun bebas. */
  protected readonly bmMaxLimit = computed(() => {
    const d = this.detail();
    const baseline = d?.checkerPurposeLimit ?? d?.engineSuggestionLimit ?? 0;
    return baseline + BM_MAX_INCREASE;
  });

  constructor() {
    this.load();
    this.destroyRef.onDestroy(() => this.revokePhotoUrls());
  }

  private load(): void {
    this.loading.set(true);
    this.errorMessage.set('');
    this.approvalApi.getDetail(this.applicationId).subscribe({
      next: (detail) => {
        this.detail.set(detail);
        this.loading.set(false);
        this.loadPhotos(detail);
      },
      error: (err) => {
        this.errorMessage.set(err.message || 'Gagal memuat detail aplikasi.');
        this.loading.set(false);
      }
    });
  }

  /** Foto KTP/selfie butuh header HMAC+Authorization (gak bisa ditempel ke <img src> polos,
   * lihat ApiService.getBlob) - fetch sebagai blob lalu jadiin object URL buat dipasang ke <img>. */
  private loadPhotos(detail: LimitApplicationDetail): void {
    this.revokePhotoUrls();
    this.ktpPhotoUrl.set(null);
    this.kycPhotoUrl.set(null);
    this.photoError.set('');

    if (detail.hasFotoKtp) {
      this.approvalApi.getPicture(this.applicationId, 'ktp').subscribe({
        next: (blob) => this.ktpPhotoUrl.set(this.toObjectUrl(blob)),
        error: () => this.photoError.set('Gagal memuat foto KTP/selfie.')
      });
    }
    if (detail.hasFotoKyc) {
      this.approvalApi.getPicture(this.applicationId, 'kyc').subscribe({
        next: (blob) => this.kycPhotoUrl.set(this.toObjectUrl(blob)),
        error: () => this.photoError.set('Gagal memuat foto KTP/selfie.')
      });
    }
  }

  private toObjectUrl(blob: Blob): string {
    const url = URL.createObjectURL(blob);
    this.objectUrls.push(url);
    return url;
  }

  private revokePhotoUrls(): void {
    this.objectUrls.forEach((url) => URL.revokeObjectURL(url));
    this.objectUrls.length = 0;
  }

  protected goBack(): void {
    void this.router.navigateByUrl(this.isBm() ? '/bm/approval' : '/approval');
  }

  protected openForm(type: ActionType): void {
    this.formOpen.set(true);
    this.actionType.set(type);
    this.actionError.set('');
    const current = this.detail();
    if (type === 'APPROVE') {
      const prefill = this.isBm() ? (current?.checkerPurposeLimit ?? current?.engineSuggestionLimit) : current?.engineSuggestionLimit;
      if (prefill != null) this.limitInput.set(prefill);
    }
  }

  protected cancelForm(): void {
    if (this.submitting()) return;
    this.formOpen.set(false);
    this.actionType.set(null);
    this.actionError.set('');
  }

  protected confirm(): void {
    const type = this.actionType();
    const current = this.detail();
    if (!type || !current || this.submitting()) return;

    const bm = this.isBm();
    const request: { action: ActionType; purposeLimit?: number; finalLimit?: number; reason?: string } = { action: type };

    if (type === 'APPROVE') {
      const limit = Number(this.limitInput());
      if (!limit || limit <= 0) {
        this.actionError.set('Nominal plafond wajib diisi.');
        return;
      }

      if (bm) {
        const baseline = current.checkerPurposeLimit ?? current.engineSuggestionLimit ?? 0;
        const maxAllowed = this.bmMaxLimit();
        if (limit > maxAllowed) {
          this.actionError.set(`Plafond final maksimal Rp ${maxAllowed.toLocaleString('id-ID')} (plafond Checker + Rp 2.000.000).`);
          return;
        }
        if (limit !== baseline && !this.reasonInput().trim()) {
          this.actionError.set('Alasan wajib diisi kalau nominal beda dari plafond Checker.');
          return;
        }
        request.finalLimit = limit;
      } else {
        if (current.checkerLimitMin != null && limit < current.checkerLimitMin) {
          this.actionError.set(`Plafond minimal Rp ${current.checkerLimitMin.toLocaleString('id-ID')}.`);
          return;
        }
        if (current.checkerLimitMax != null && limit > current.checkerLimitMax) {
          this.actionError.set(`Plafond maksimal Rp ${current.checkerLimitMax.toLocaleString('id-ID')}.`);
          return;
        }
        request.purposeLimit = limit;
      }
    } else if (!this.reasonInput().trim()) {
      this.actionError.set('Alasan reject wajib diisi.');
      return;
    }

    const reason = this.reasonInput().trim();
    if (reason) request.reason = reason;

    this.submitting.set(true);
    this.actionError.set('');
    const decide$ = bm ? this.approvalApi.bmDecide(this.applicationId, request) : this.approvalApi.checkerDecide(this.applicationId, request);
    decide$.subscribe({
      next: () => void this.router.navigateByUrl(bm ? '/bm/approval' : '/approval'),
      error: (err) => {
        this.submitting.set(false);
        this.actionError.set(err.message || 'Gagal menyimpan keputusan.');
      }
    });
  }
}
