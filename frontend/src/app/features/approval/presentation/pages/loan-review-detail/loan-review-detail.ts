import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ApprovalApiService } from '../../../infrastructure/approval-api.service';
import { LoanReviewDetail } from '../../../domain/entities/limit-application';

type ActionType = 'APPROVE' | 'REJECT';

/**
 * Detail + keputusan BM untuk pengajuan pinjaman yang melewati 30% dari plafond
 * (LoanReviewRequest, skip Checker - konfirmasi user). Lebih sederhana dari ApprovalDetailPage:
 * tidak ada negosiasi nominal sama sekali, BM cuma Approve (persis sesuai amount yang diminta)
 * atau Reject (reason wajib).
 */
@Component({
  selector: 'app-loan-review-detail',
  imports: [DatePipe, FormsModule],
  templateUrl: './loan-review-detail.html',
  styleUrl: './loan-review-detail.css'
})
export class LoanReviewDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly approvalApi = inject(ApprovalApiService);

  protected readonly reviewId = Number(this.route.snapshot.paramMap.get('id'));

  protected readonly detail = signal<LoanReviewDetail | null>(null);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal('');

  protected readonly formOpen = signal(false);
  protected readonly actionType = signal<ActionType | null>(null);
  protected readonly reasonInput = signal('');
  protected readonly submitting = signal(false);
  protected readonly actionError = signal('');

  protected readonly canDecide = computed(() => this.detail()?.status === 'PENDING_BM');

  constructor() {
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.errorMessage.set('');
    this.approvalApi.getLoanReviewDetail(this.reviewId).subscribe({
      next: (detail) => {
        this.detail.set(detail);
        this.loading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err.message || 'Gagal memuat detail pengajuan.');
        this.loading.set(false);
      }
    });
  }

  protected goBack(): void {
    void this.router.navigateByUrl('/bm/approval');
  }

  protected openForm(type: ActionType): void {
    this.formOpen.set(true);
    this.actionType.set(type);
    this.actionError.set('');
  }

  protected cancelForm(): void {
    if (this.submitting()) return;
    this.formOpen.set(false);
    this.actionType.set(null);
    this.actionError.set('');
  }

  protected confirm(): void {
    const type = this.actionType();
    if (!type || this.submitting()) return;

    if (type === 'REJECT' && !this.reasonInput().trim()) {
      this.actionError.set('Alasan reject wajib diisi.');
      return;
    }

    const reason = this.reasonInput().trim();
    this.submitting.set(true);
    this.actionError.set('');
    this.approvalApi.loanReviewBmDecide(this.reviewId, { action: type, reason: reason || undefined }).subscribe({
      next: () => void this.router.navigateByUrl('/bm/approval'),
      error: (err) => {
        this.submitting.set(false);
        this.actionError.set(err.message || 'Gagal menyimpan keputusan.');
      }
    });
  }
}
