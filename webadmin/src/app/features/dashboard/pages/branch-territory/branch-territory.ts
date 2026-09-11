import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TableComponent } from '../../../../shared/components/table/table';
import { TableCellDirective } from '../../../../shared/components/table/table-cell.directive';
import { TableColumn } from '../../../../shared/components/table/table-column';
import { ModalComponent } from '../../../../shared/components/modal/modal';
import { Dbo } from '../../../../core/services/dbo/dbo';
import { BranchTerritoryServices } from '../../services/branch-territory-services';
import { BranchOption, BranchTerritory, RegencyGap } from '../../models/branch-territory.model';
import { ProvinceResponse, RegenciesResponse } from '../../../../shared/models/dbo-response';

/** Province id Pulau Jawa (konfirmasi user) - cuma buat UX (tampilin dropdown regency vs bulk-province). Validasi beneran tetap di backend. */
const JAVA_PROVINCE_IDS = new Set([31, 32, 33, 34, 35, 36]);
const JAKARTA_PROVINCE_ID = 31;

@Component({
  selector: 'app-branch-territory',
  standalone: true,
  imports: [FormsModule, TableComponent, TableCellDirective, ModalComponent],
  templateUrl: './branch-territory.html',
  styleUrl: './branch-territory.css',
})
export class BranchTerritoryPage implements OnInit {
  private readonly territoryServices = inject(BranchTerritoryServices);
  private readonly dbo = inject(Dbo);

  readonly territories = signal<BranchTerritory[]>([]);
  readonly gaps = signal<RegencyGap[]>([]);
  readonly branches = signal<BranchOption[]>([]);
  readonly provinces = signal<ProvinceResponse[]>([]);
  readonly regencies = signal<RegenciesResponse[]>([]);

  readonly isLoading = signal(false);
  readonly errorMessage = signal('');
  readonly showGaps = signal(false);

  readonly assignOpen = signal(false);
  readonly selectedBranchId = signal<number | null>(null);
  readonly selectedProvinceId = signal<number | null>(null);
  readonly selectedRegencyId = signal<number | null>(null);
  readonly isSaving = signal(false);
  readonly assignError = signal('');

  readonly isJavaProvince = computed(() => {
    const id = this.selectedProvinceId();
    return id != null && JAVA_PROVINCE_IDS.has(id);
  });

  readonly isJakarta = computed(() => this.selectedProvinceId() === JAKARTA_PROVINCE_ID);

  readonly canSubmitAssign = computed(() => {
    if (this.isSaving() || !this.selectedBranchId() || !this.selectedProvinceId()) return false;
    return this.isJavaProvince() ? !!this.selectedRegencyId() : true;
  });

  readonly columns: TableColumn<BranchTerritory>[] = [
    { key: 'regencyName', header: 'Regency', width: '30%' },
    { key: 'provinceName', header: 'Provinsi', width: '25%' },
    { key: 'branchName', header: 'Branch', width: '30%' },
    { key: 'actions', header: 'Aksi', headerClass: 'col-center', cellClass: 'col-center', width: '15%' },
  ];

  readonly trackById = (row: BranchTerritory) => row.id;

  get assignModalOpen(): boolean {
    return this.assignOpen();
  }
  set assignModalOpen(value: boolean) {
    if (!value) this.closeAssign();
  }

  ngOnInit(): void {
    this.loadTerritories();
    this.loadGaps();
  }

  loadTerritories(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.territoryServices.getAll().subscribe({
      next: (response) => {
        this.territories.set(response.success ? response.data ?? [] : []);
        if (!response.success) this.errorMessage.set(response.message || 'Gagal memuat branch territory.');
        this.isLoading.set(false);
      },
      error: () => {
        this.territories.set([]);
        this.errorMessage.set('Gagal memuat branch territory.');
        this.isLoading.set(false);
      },
    });
  }

  loadGaps(): void {
    this.territoryServices.getGaps().subscribe({
      next: (response) => this.gaps.set(response.success ? response.data ?? [] : []),
      error: () => this.gaps.set([]),
    });
  }

  toggleGaps(): void {
    this.showGaps.update((value) => !value);
  }

  openAssign(): void {
    this.assignOpen.set(true);
    this.assignError.set('');
    this.selectedBranchId.set(null);
    this.selectedProvinceId.set(null);
    this.selectedRegencyId.set(null);
    this.regencies.set([]);

    if (!this.branches().length) {
      this.territoryServices.listBranches().subscribe({
        next: (branches) => this.branches.set(branches),
        error: () => this.assignError.set('Gagal memuat daftar branch.'),
      });
    }
    if (!this.provinces().length) {
      this.dbo.getAllProvinces().subscribe({
        next: (response) => this.provinces.set(response.success ? response.data ?? [] : []),
        error: () => this.assignError.set('Gagal memuat daftar provinsi.'),
      });
    }
  }

  closeAssign(): void {
    if (!this.isSaving()) this.assignOpen.set(false);
  }

  onProvinceChange(value: string): void {
    const provinceId = value ? Number(value) : null;
    this.selectedProvinceId.set(provinceId);
    this.selectedRegencyId.set(null);
    this.regencies.set([]);

    if (provinceId != null && JAVA_PROVINCE_IDS.has(provinceId)) {
      this.dbo.getAllRegencies(provinceId).subscribe({
        next: (response) => this.regencies.set(response.success ? response.data ?? [] : []),
        error: () => this.assignError.set('Gagal memuat daftar regency.'),
      });
    }
  }

  submitAssign(): void {
    const branchId = this.selectedBranchId();
    const provinceId = this.selectedProvinceId();
    if (!branchId || !provinceId || this.isSaving()) return;

    this.isSaving.set(true);
    this.assignError.set('');

    if (this.isJavaProvince()) {
      this.territoryServices.assignRegency(branchId, this.selectedRegencyId()!).subscribe({
        next: (response) => this.handleAssignResult(response.success, response.message),
        error: (err) => this.handleAssignError(err),
      });
    } else {
      this.territoryServices.assignProvince(branchId, provinceId).subscribe({
        next: (response) => this.handleAssignResult(response.success, response.message),
        error: (err) => this.handleAssignError(err),
      });
    }
  }

  private handleAssignResult(success: boolean, message: string): void {
    this.isSaving.set(false);
    if (success) {
      this.assignOpen.set(false);
      this.loadTerritories();
      this.loadGaps();
    } else {
      this.assignError.set(message || 'Gagal assign territory.');
    }
  }

  private handleAssignError(err: unknown): void {
    this.isSaving.set(false);
    const message = (err as { error?: { message?: string } })?.error?.message;
    this.assignError.set(message || 'Gagal assign territory.');
  }

  remove(item: BranchTerritory): void {
    if (!confirm(`Hapus ${item.regencyName} dari territory ${item.branchName}?`)) return;
    this.territoryServices.unassign(item.id).subscribe({
      next: () => this.loadTerritories(),
      error: () => this.errorMessage.set('Gagal menghapus territory.'),
    });
  }
}
