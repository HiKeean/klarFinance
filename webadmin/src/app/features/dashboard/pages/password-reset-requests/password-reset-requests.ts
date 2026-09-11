import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { TableComponent } from '../../../../shared/components/table/table';
import { TableCellDirective } from '../../../../shared/components/table/table-cell.directive';
import { TableColumn } from '../../../../shared/components/table/table-column';
import { PasswordResetRequestServices } from '../../services/password-reset-request-services';
import { PasswordResetRequestItem } from '../../models/password-reset-request.model';

type StatusFilter = 'PENDING' | 'APPROVED' | 'REJECTED';

/** Antrean "Lupa Password" - dipicu dari tombol reset password di halaman login Checker&BM
 * (muncul setelah 3x salah password). Approve = generate password baru random + kirim WhatsApp
 * ke nomor HP staff (lihat backend PasswordResetRequestService), reject = ditolak dengan alasan. */
@Component({
  selector: 'app-password-reset-requests',
  standalone: true,
  imports: [DatePipe, TableComponent, TableCellDirective],
  templateUrl: './password-reset-requests.html',
  styleUrl: './password-reset-requests.css',
})
export class PasswordResetRequestsPage implements OnInit {
  private readonly services = inject(PasswordResetRequestServices);

  readonly requests = signal<PasswordResetRequestItem[]>([]);
  readonly isLoading = signal(false);
  readonly errorMessage = signal('');
  readonly statusFilter = signal<StatusFilter>('PENDING');
  readonly decidingId = signal<number | null>(null);

  readonly columns: TableColumn<PasswordResetRequestItem>[] = [
    { key: 'identity', header: 'Identity', width: '15%' },
    { key: 'name', header: 'Nama', width: '20%' },
    { key: 'role', header: 'Role', width: '12%' },
    { key: 'requestedAt', header: 'Diajukan', width: '18%' },
    { key: 'status', header: 'Status', width: '13%' },
    { key: 'actions', header: 'Aksi', headerClass: 'col-center', cellClass: 'col-center', width: '22%' },
  ];

  readonly trackById = (row: PasswordResetRequestItem) => row.id;

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.services.getAll(this.statusFilter()).subscribe({
      next: (response) => {
        this.requests.set(response.success ? response.data ?? [] : []);
        if (!response.success) this.errorMessage.set(response.message || 'Gagal memuat permintaan reset password.');
        this.isLoading.set(false);
      },
      error: () => {
        this.requests.set([]);
        this.errorMessage.set('Gagal memuat permintaan reset password.');
        this.isLoading.set(false);
      },
    });
  }

  changeFilter(status: StatusFilter): void {
    this.statusFilter.set(status);
    this.load();
  }

  approve(item: PasswordResetRequestItem): void {
    if (!confirm(`Approve reset password untuk ${item.identity}? Password baru akan dikirim ke WhatsApp staff ini.`)) return;
    this.decidingId.set(item.id);
    this.services.decide(item.id, 'APPROVE').subscribe({
      next: (response) => {
        this.decidingId.set(null);
        if (response.success) {
          this.load();
        } else {
          alert(response.message || 'Gagal approve permintaan.');
        }
      },
      error: (err) => {
        this.decidingId.set(null);
        alert((err as { error?: { message?: string } })?.error?.message || 'Gagal approve permintaan.');
      },
    });
  }

  reject(item: PasswordResetRequestItem): void {
    const reason = prompt(`Alasan menolak reset password untuk ${item.identity}:`);
    if (!reason || !reason.trim()) return;
    this.decidingId.set(item.id);
    this.services.decide(item.id, 'REJECT', reason.trim()).subscribe({
      next: (response) => {
        this.decidingId.set(null);
        if (response.success) {
          this.load();
        } else {
          alert(response.message || 'Gagal menolak permintaan.');
        }
      },
      error: (err) => {
        this.decidingId.set(null);
        alert((err as { error?: { message?: string } })?.error?.message || 'Gagal menolak permintaan.');
      },
    });
  }
}
