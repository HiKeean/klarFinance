import { DatePipe } from '@angular/common';
import { Component, DestroyRef, effect, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { AuthStateService } from '../../../../../core/services/auth-state.service';
import { CheckerRealtimeService } from '../../../../../core/services/checker-realtime.service';
import { ApprovalApiService } from '../../../infrastructure/approval-api.service';
import { LimitApplicationSummary } from '../../../domain/entities/limit-application';

@Component({
  selector: 'app-approval-list',
  imports: [DatePipe],
  templateUrl: './approval-list.html',
  styleUrl: './approval-list.css'
})
export class ApprovalListPage {
  private readonly authState = inject(AuthStateService);
  private readonly approvalApi = inject(ApprovalApiService);
  private readonly realtime = inject(CheckerRealtimeService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);

  private readonly userProfile = toSignal(this.authState.userProfile$, { initialValue: null });
  protected readonly isBm = () => this.userProfile()?.role?.toUpperCase() === 'BM';
  private readonly isChecker = () => this.userProfile()?.role?.toUpperCase() === 'CHECKER';

  protected readonly items = signal<LimitApplicationSummary[]>([]);
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly realtimeNotice = signal('');
  protected readonly breaking = signal(false);

  private realtimeStarted = false;

  constructor() {
    this.load();

    effect(() => {
      if (this.isChecker() && !this.realtimeStarted) {
        this.realtimeStarted = true;
        void this.realtime.connect();
      }
    });

    this.realtime.notifications$.pipe().subscribe((notification) => {
      this.realtimeNotice.set(notification.message);
      this.load();
    });

    // Re-sync tiap kali WS (re-)connect - ada race antara backend assign aplikasi begitu
    // markOnline() jalan (server terima koneksi) dengan client selesai subscribe ke
    // /user/queue/assignment; assignment yang kejadian pas jendela itu (atau pas WS sempat
    // putus/reconnect) bisa gak kekirim notifnya. load() awal di atas jalan sebelum WS sempat
    // connect sama sekali, jadi gak pernah lihat assignment yang baru kejadian abis itu.
    this.realtime.connected$.subscribe(() => this.load());

    this.destroyRef.onDestroy(() => this.realtime.disconnect());
  }

  /**
   * Checker gak pernah lihat list/tabel - begitu ada aplikasi ke-assign ke dia, langsung
   * diarahkan ke halaman detail-nya (konfirmasi user). BM tetap lihat tabel (bucket-per-branch,
   * lihat bm-approval-lock.md) - setiap row nunjukin status lock BM lain lewat lockedByIdentity.
   */
  private load(): void {
    this.loading.set(true);
    this.errorMessage.set('');
    this.approvalApi.getQueue().subscribe({
      next: (items) => {
        this.items.set(items);
        this.loading.set(false);
        if (!this.isBm() && items.length > 0) {
          void this.router.navigate(['/approval', items[0].id]);
        }
      },
      error: (err) => {
        this.errorMessage.set(err.message || 'Gagal memuat antrean.');
        this.loading.set(false);
      }
    });
  }

  /** Tombol "Istirahat" (konfirmasi user) - beda dari sekadar tutup tab: full release paksa
   * apapun yang lagi dipegang di Redis (bukan cuma disconnect pasif), baru putus WS & balik ke dashboard. */
  protected takeBreak(): void {
    if (this.breaking()) return;
    this.breaking.set(true);
    this.approvalApi.checkerBreak().subscribe({
      next: () => {
        this.realtime.disconnect();
        void this.router.navigateByUrl('/dashboard');
      },
      error: (err) => {
        this.breaking.set(false);
        this.errorMessage.set(err.message || 'Gagal mengambil istirahat.');
      }
    });
  }

  /**
   * BM klik row - masuk ke halaman detail (ini yang men-trigger lock permanen tanpa TTL di
   * backend, lihat bm-approval-lock.md). Kalau udah dikunci BM lain, tombolnya sengaja disabled
   * (lihat template) jadi guard ini cuma defensif kalau ada race sebelum re-render.
   *
   * `type` membedakan tujuan navigasi: LIMIT_APPLICATION (pengajuan plafond baru) tetap ke
   * halaman detail lama, LOAN_REVIEW (pengajuan pinjaman >30%) ke halaman baru yang lebih
   * sederhana (tidak ada negosiasi nominal) - lihat LoanReviewDetailPage.
   */
  protected openDetail(item: LimitApplicationSummary): void {
    if (item.lockedByIdentity && !item.lockedByMe) return;
    if (item.type === 'LOAN_REVIEW') {
      void this.router.navigate(['/bm/loan-review', item.id]);
      return;
    }
    void this.router.navigate(['/bm/approval', item.id]);
  }
}
