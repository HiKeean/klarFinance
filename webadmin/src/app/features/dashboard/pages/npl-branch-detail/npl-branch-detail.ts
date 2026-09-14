import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged, Subject } from 'rxjs';
import { TableComponent } from '../../../../shared/components/table/table';
import { TableCellDirective } from '../../../../shared/components/table/table-cell.directive';
import { TableColumn } from '../../../../shared/components/table/table-column';
import { NplReportServices } from '../../services/npl-report-services';
import { BranchLoanDetailItem } from '../../models/npl-report.model';

function formatRupiah(amount: number): string {
  return `Rp ${amount.toLocaleString('id-ID')}`;
}

@Component({
  selector: 'app-npl-branch-detail',
  standalone: true,
  imports: [RouterLink, TableComponent, TableCellDirective],
  templateUrl: './npl-branch-detail.html',
  styleUrl: './npl-branch-detail.css',
})
export class NplBranchDetailPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly nplReportServices = inject(NplReportServices);
  private readonly destroyRef = inject(DestroyRef);
  private readonly searchInput$ = new Subject<string>();
  private readonly pageSize = 10;

  private readonly branchId = Number(this.route.snapshot.paramMap.get('branchId'));

  readonly branchName = signal('');
  readonly totalAssets = signal(0);
  readonly activeBorrowers = signal(0);

  readonly loans = signal<BranchLoanDetailItem[]>([]);
  readonly pageIndex = signal(0);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly isLoading = signal(false);
  readonly errorMessage = signal('');
  readonly searchTerm = signal('');

  readonly columns: TableColumn<BranchLoanDetailItem>[] = [
    { key: 'nasabahName', header: 'Nasabah', width: '35%' },
    { key: 'loanAmount', header: 'Nominal Pinjaman', width: '25%' },
    { key: 'status', header: 'Status', width: '20%' },
    { key: 'daysOverdue', header: 'Overdue', width: '20%' },
  ];

  readonly trackByLoanId = (row: BranchLoanDetailItem) => row.loanId;

  ngOnInit(): void {
    this.searchInput$
      .pipe(debounceTime(400), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe((search) => {
        this.pageIndex.set(0);
        this.loadLoans(search);
      });

    this.loadLoans();
  }

  formatAmount(amount: number): string {
    return formatRupiah(amount);
  }

  statusLabel(status: string): string {
    return status === 'Current' ? 'Sehat' : status;
  }

  statusClass(status: string): string {
    if (status === 'Overdue') return 'badge-overdue';
    if (status === 'Lunas') return 'badge-lunas';
    return 'badge-current';
  }

  loadLoans(search = this.searchTerm().trim()): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.nplReportServices.getBranchLoans(this.branchId, this.pageIndex(), this.pageSize, search).subscribe({
      next: (response) => {
        if (response.success && response.data) {
          this.branchName.set(response.data.branchName);
          this.totalAssets.set(response.data.totalAssets);
          this.activeBorrowers.set(response.data.activeBorrowers);
          this.loans.set(response.data.loans?.content ?? []);
          this.totalElements.set(response.data.loans?.totalElements ?? 0);
          this.totalPages.set(response.data.loans?.totalPages ?? 0);
        } else {
          this.errorMessage.set(response.message || 'Gagal memuat data pinjaman branch.');
        }
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Gagal memuat data pinjaman branch.');
        this.isLoading.set(false);
      },
    });
  }

  onSearch(value: string): void {
    this.searchTerm.set(value);
    this.searchInput$.next(value.trim());
  }

  previousPage(): void {
    if (this.pageIndex() === 0) return;
    this.pageIndex.update((page) => page - 1);
    this.loadLoans();
  }

  nextPage(): void {
    if (this.pageIndex() >= this.totalPages() - 1) return;
    this.pageIndex.update((page) => page + 1);
    this.loadLoans();
  }

  goToPage(page: number): void {
    if (page === this.pageIndex() || page < 0 || page >= this.totalPages()) return;
    this.pageIndex.set(page);
    this.loadLoans();
  }

  visiblePages(): number[] {
    const total = this.totalPages();
    const current = this.pageIndex();
    if (total <= 5) return Array.from({ length: total }, (_, index) => index);
    if (current <= 2) return [0, 1, 2, 3, 4];
    if (current >= total - 3) return [total - 5, total - 4, total - 3, total - 2, total - 1];
    return [current - 2, current - 1, current, current + 1, current + 2];
  }

  get startEntry(): number {
    return this.totalElements() === 0 ? 0 : this.pageIndex() * this.pageSize + 1;
  }

  get endEntry(): number {
    return Math.min((this.pageIndex() + 1) * this.pageSize, this.totalElements());
  }
}
