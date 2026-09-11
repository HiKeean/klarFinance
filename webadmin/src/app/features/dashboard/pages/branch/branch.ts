import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { TableComponent } from '../../../../shared/components/table/table';
import { TableCellDirective } from '../../../../shared/components/table/table-cell.directive';
import { TableColumn } from '../../../../shared/components/table/table-column';
import { ModalComponent } from '../../../../shared/components/modal/modal';
import { DropdownComponent, DropdownOption } from '../../../../shared/components/dropdown/dropdown';
import { BranchServices } from '../../services/branch-services';
import { BmOption, BranchListItem } from '../../models/branch.model';

@Component({
  selector: 'app-branch',
  standalone: true,
  imports: [FormsModule, TableComponent, TableCellDirective, ModalComponent, DropdownComponent],
  templateUrl: './branch.html',
  styleUrl: './branch.css',
})
export class BranchPage implements OnInit {
  private readonly branchServices = inject(BranchServices);

  readonly branches = signal<BranchListItem[]>([]);
  readonly bms = signal<BmOption[]>([]);
  readonly isLoading = signal(false);
  readonly errorMessage = signal('');

  readonly assignOpen = signal(false);
  readonly activeBranch = signal<BranchListItem | null>(null);
  readonly selectedBmIdentity = signal<string | null>(null);
  readonly isSaving = signal(false);
  readonly assignError = signal('');

  readonly bmOptions = computed<DropdownOption<string>[]>(() =>
    this.bms().map((bm) => ({
      label: bm.branchName ? `${bm.name ?? bm.identity} (${bm.identity}) — saat ini: ${bm.branchName}` : `${bm.name ?? bm.identity} (${bm.identity})`,
      value: bm.identity,
    })),
  );

  readonly columns: TableColumn<BranchListItem>[] = [
    { key: 'name', header: 'Branch', width: '25%' },
    { key: 'villageName', header: 'Wilayah', width: '20%' },
    { key: 'address', header: 'Alamat', width: '25%' },
    { key: 'bm', header: 'BM', width: '20%' },
    { key: 'actions', header: 'Aksi', headerClass: 'col-center', cellClass: 'col-center', width: '10%' },
  ];

  readonly trackByBranchCode = (row: BranchListItem) => row.branchCode;

  get assignModalOpen(): boolean {
    return this.assignOpen();
  }
  set assignModalOpen(value: boolean) {
    if (!value) this.closeAssign();
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    forkJoin({
      branches: this.branchServices.listBranches(),
      bms: this.branchServices.listBms(),
    }).subscribe({
      next: ({ branches, bms }) => {
        this.bms.set(bms);
        this.branches.set(
          branches.map((branch) => {
            const bm = bms.find((candidate) => candidate.branchCode === branch.branchCode);
            return { ...branch, bmIdentity: bm?.identity ?? null, bmName: bm?.name ?? null };
          }),
        );
        this.isLoading.set(false);
      },
      error: () => {
        this.branches.set([]);
        this.errorMessage.set('Gagal memuat daftar branch.');
        this.isLoading.set(false);
      },
    });
  }

  openAssign(branch: BranchListItem): void {
    this.activeBranch.set(branch);
    this.selectedBmIdentity.set(branch.bmIdentity);
    this.assignError.set('');
    this.assignOpen.set(true);
  }

  closeAssign(): void {
    if (!this.isSaving()) this.assignOpen.set(false);
  }

  submitAssign(): void {
    const branch = this.activeBranch();
    const bmIdentity = this.selectedBmIdentity();
    if (!branch || !bmIdentity || this.isSaving()) return;

    this.isSaving.set(true);
    this.assignError.set('');
    this.branchServices.assignBranch(bmIdentity, branch.branchCode).subscribe({
      next: (response) => {
        this.isSaving.set(false);
        if (!response.success) {
          this.assignError.set(response.message || 'Gagal assign BM.');
          return;
        }
        this.assignOpen.set(false);
        this.load();
      },
      error: (err) => {
        this.isSaving.set(false);
        this.assignError.set((err as { error?: { message?: string } })?.error?.message || 'Gagal assign BM.');
      },
    });
  }

  unassign(branch: BranchListItem): void {
    if (!branch.bmIdentity) return;
    if (!confirm(`Lepas ${branch.bmName || branch.bmIdentity} dari branch ${branch.name}?`)) return;
    this.branchServices.assignBranch(branch.bmIdentity, null).subscribe({
      next: () => this.load(),
      error: () => this.errorMessage.set('Gagal melepas BM.'),
    });
  }
}
