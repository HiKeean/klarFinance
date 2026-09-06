import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { SearchInputComponent } from '../../../../../shared/components/search-input/search-input';
import { InquiryApiService } from '../../../infrastructure/inquiry-api.service';
import { InquiryResult } from '../../../domain/entities/inquiry-result';

const STATUS_LABEL: Record<string, string> = {
  PENDING_CHECKER: 'Menunggu Checker',
  PENDING_BM: 'Menunggu BM',
  APPROVED: 'Disetujui',
  REJECTED: 'Ditolak'
};

@Component({
  selector: 'app-inquiry-list',
  imports: [SearchInputComponent],
  templateUrl: './inquiry-list.html',
  styleUrl: './inquiry-list.css'
})
export class InquiryListPage {
  private readonly inquiryApi = inject(InquiryApiService);
  private readonly router = inject(Router);

  protected readonly results = signal<InquiryResult[]>([]);
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly hasSearched = signal(false);

  protected statusLabel(status: string): string {
    return STATUS_LABEL[status] || status;
  }

  protected onSearch(query: string): void {
    if (!query) {
      this.results.set([]);
      this.hasSearched.set(false);
      this.errorMessage.set('');
      return;
    }

    this.hasSearched.set(true);
    this.loading.set(true);
    this.errorMessage.set('');
    this.inquiryApi.search(query).subscribe({
      next: (results) => {
        this.results.set(results);
        this.loading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err.message || 'Gagal mencari aplikasi.');
        this.loading.set(false);
      }
    });
  }

  protected openDetail(item: InquiryResult): void {
    void this.router.navigate(['/approval', item.id]);
  }
}
