import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ReactiveFormsModule, FormsModule, FormBuilder, Validators } from '@angular/forms';
import { debounceTime, distinctUntilChanged, Subject } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Store } from '@ngrx/store';
import * as RegistrationActions from './user-registration.actions';
import { selectBranches } from './user-registration.selectors';
import { UserRegistrationState } from './user-registration.reducer';
import { User as UserModel } from '../../models/user.model';
import { RegisterEmployeeRequest } from '../../models/registration.model';
import { UserServices } from '../../services/user-services';
import { Dbo } from '../../../../core/services/dbo/dbo';
import { BranchResponse, DistrictsResponse, ProvinceResponse, RegenciesResponse, VillagesResponse } from '../../../../shared/models/dbo-response';
import { DropdownComponent, DropdownOption } from '../../../../shared/components/dropdown/dropdown';
import { TableComponent } from '../../../../shared/components/table/table';
import { TableCellDirective } from '../../../../shared/components/table/table-cell.directive';
import { TableColumn } from '../../../../shared/components/table/table-column';
import { ModalComponent } from '../../../../shared/components/modal/modal';

@Component({
  selector: 'app-user',
  imports: [
    ReactiveFormsModule,
    FormsModule,
    DropdownComponent,
    TableComponent,
    TableCellDirective,
    ModalComponent,
  ],
  templateUrl: './user.html',
  styleUrl: './user.css',
  standalone: true,
})
export class User implements OnInit {
  private readonly userServices = inject(UserServices);
  // private readonly authApi = inject(AuthApiService);
  private readonly dbo = inject(Dbo);
  private readonly formBuilder = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly searchInput$ = new Subject<string>();
  private readonly store = inject(Store<{ userRegistration: UserRegistrationState }>);
  private readonly pageSize = 10;

  readonly users = signal<UserModel[]>([]);
  readonly pageIndex = signal(0);
  readonly totalElements = signal(0);
  readonly totalPages = signal(0);
  readonly isLoading = signal(false);
  readonly searchTerm = signal('');
  readonly roleFilter = signal('');
  readonly isModalOpen = signal(false);
  readonly isSaving = signal(false);
  readonly deletingIdentity = signal<string | null>(null);
  readonly formError = signal('');
  readonly isSuccessOpen = signal(false);
  readonly successMessage = signal('Account berhasil diregistrasi.');
  readonly branches = this.store.selectSignal(selectBranches);
  readonly roleOptions: DropdownOption<string>[] = [
    { label: 'BM', value: 'BM' },
    { label: 'FINANCE', value: 'FINANCE' },
    { label: 'CHECKER', value: 'CHECKER' },
  ];
  readonly roleFilterOptions: DropdownOption<string>[] = [
    { label: 'All Roles', value: '' },
    ...this.roleOptions,
  ];
  readonly branchOptions = () => this.branches().map((branch) => ({
    label: branch.name,
    value: branch.branchCode,
  }));

  readonly employeeForm = this.formBuilder.group({
    name: ['', Validators.required],
    role: ['', Validators.required],
    noHp: ['', [Validators.required, Validators.pattern(/^\d+$/)]],
    dob: ['', Validators.required],
    password: ['', [Validators.required, Validators.minLength(6)]],
    branchId: [null as number | null, Validators.required],
    provinceId: [null as number | null, Validators.required],
    regencyId: [{ value: null as number | null, disabled: true }, Validators.required],
    districtId: [{ value: null as number | null, disabled: true }, Validators.required],
    villageId: [{ value: null as number | null, disabled: true }, Validators.required],
    address: ['', Validators.required],
  });

  readonly provinceOptions = signal<DropdownOption<number>[]>([]);
  readonly regencyOptions = signal<DropdownOption<number>[]>([]);
  readonly districtOptions = signal<DropdownOption<number>[]>([]);
  readonly villageOptions = signal<DropdownOption<number>[]>([]);

  readonly filteredUsers = computed(() => this.users());

  readonly columns: TableColumn<UserModel>[] = [
    { key: 'identity', header: 'Identity', cellClass: 'col-mono', width: '17%' },
    { key: 'name', header: 'Name', width: '19%' },
    { key: 'role', header: 'Role', width: '17%' },
    { key: 'noHp', header: 'No HP', width: '21%' },
    { key: 'branch.name', header: 'Branch', width: '17%' },
    { key: 'actions', header: 'Actions', headerClass: 'col-center', cellClass: 'col-center', width: '9%' },
  ];

  readonly trackByIdentity = (row: UserModel, index: number) => row.identity ?? index;

  readonly rowClass = (row: UserModel) => (row.deletedAt ? 'row-muted' : '');

  get registrationModalOpen(): boolean {
    return this.isModalOpen();
  }

  set registrationModalOpen(value: boolean) {
    if (!value) this.closeRegistration();
  }

  ngOnInit(): void {
    this.searchInput$
      .pipe(debounceTime(400), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe((search) => {
        this.pageIndex.set(0);
        this.loadUsers(search);
      });

    this.loadUsers();
    this.employeeForm.controls.provinceId.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((provinceId) => this.onProvinceChanged(provinceId));
    this.employeeForm.controls.regencyId.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((regencyId) => this.onRegencyChanged(regencyId));
    this.employeeForm.controls.districtId.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((districtId) => this.onDistrictChanged(districtId));
  }

  openRegistration(): void {
    this.formError.set('');
    this.resetRegistrationForm();
    this.isModalOpen.set(true);
    this.dbo.getAllBranches().subscribe({
      next: (response) => {
        this.store.dispatch(
          RegistrationActions.setBranches({
            items: this.toList<BranchResponse>(response.data),
          }),
        );
      },
      error: () => this.formError.set('Unable to load branch data.'),
    });
    this.loadProvinces();

    // forkJoin({
    //   // branches: this.dbo.getBranches(0, 10),
    // })
    //   .pipe(timeout(10_000), takeUntilDestroyed(this.destroyRef))
    //   .subscribe({
    //     next: ({ branches }) => {
    //       const data = branches.data as BranchResponse[] | { content: BranchResponse[] };
    //       this.store.dispatch(
    //         RegistrationActions.registrationOptionsLoaded({
    //           branches: Array.isArray(data) ? data : (data.content ?? []),
    //           provinces: [],
    //         }),
    //       );
    //       this.resetRegistrationForm();
    //       this.isModalOpen.set(true);
    //     },
    //     error: () => {
    //       this.store.dispatch(
    //         RegistrationActions.registrationOptionsFailed({
    //           error: 'Unable to load branch and location data. Please try again.',
    //         }),
    //       );
    //     },
    //   });
  }

  private resetRegistrationForm(): void {
    this.employeeForm.reset();
    this.employeeForm.patchValue(
      { provinceId: null, regencyId: null, districtId: null, villageId: null },
      { emitEvent: false },
    );
    this.regencyOptions.set([]);
    this.districtOptions.set([]);
    this.villageOptions.set([]);
    this.employeeForm.controls.regencyId.disable({ emitEvent: false });
    this.employeeForm.controls.districtId.disable({ emitEvent: false });
    this.employeeForm.controls.villageId.disable({ emitEvent: false });
  }

  private loadProvinces(): void {
    this.dbo.getAllProvinces().subscribe({
      next: (response) => this.provinceOptions.set(this.toOptions(this.toList<ProvinceResponse>(response.data))),
      error: () => this.formError.set('Unable to load province data.'),
    });
  }

  private onProvinceChanged(provinceId: number | null): void {
    this.employeeForm.patchValue({ regencyId: null, districtId: null, villageId: null }, { emitEvent: false });
    this.regencyOptions.set([]);
    this.districtOptions.set([]);
    this.villageOptions.set([]);
    this.employeeForm.controls.regencyId.disable({ emitEvent: false });
    this.employeeForm.controls.districtId.disable({ emitEvent: false });
    this.employeeForm.controls.villageId.disable({ emitEvent: false });
    if (provinceId === null) return;
    this.employeeForm.controls.regencyId.enable({ emitEvent: false });
    this.dbo.getAllRegencies(provinceId).subscribe({
      next: (response) => this.regencyOptions.set(this.toOptions(this.toList<RegenciesResponse>(response.data))),
      error: () => this.formError.set('Unable to load regency data.'),
    });
  }

  private onRegencyChanged(regencyId: number | null): void {
    this.employeeForm.patchValue({ districtId: null, villageId: null }, { emitEvent: false });
    this.districtOptions.set([]);
    this.villageOptions.set([]);
    this.employeeForm.controls.districtId.disable({ emitEvent: false });
    this.employeeForm.controls.villageId.disable({ emitEvent: false });
    if (regencyId === null) return;
    this.employeeForm.controls.districtId.enable({ emitEvent: false });
    this.dbo.getAllDistricts(regencyId).subscribe({
      next: (response) => this.districtOptions.set(this.toOptions(this.toList<DistrictsResponse>(response.data))),
      error: () => this.formError.set('Unable to load district data.'),
    });
  }

  private onDistrictChanged(districtId: number | null): void {
    this.employeeForm.patchValue({ villageId: null }, { emitEvent: false });
    this.villageOptions.set([]);
    this.employeeForm.controls.villageId.disable({ emitEvent: false });
    if (districtId === null) return;
    this.employeeForm.controls.villageId.enable({ emitEvent: false });
    this.dbo.getAllVillages(districtId).subscribe({
      next: (response) => this.villageOptions.set(this.toOptions(this.toList<VillagesResponse>(response.data))),
      error: () => this.formError.set('Unable to load village data.'),
    });
  }

  private toOptions(items: Array<ProvinceResponse | RegenciesResponse | DistrictsResponse | VillagesResponse>): DropdownOption<number>[] {
    return items.map((item) => ({ label: item.name, value: item.id }));
  }

  private toList<T>(data: unknown): T[] {
    if (Array.isArray(data)) return data as T[];
    if (data && typeof data === 'object' && 'content' in data) {
      const content = (data as { content?: unknown }).content;
      return Array.isArray(content) ? content as T[] : [];
    }
    return [];
  }

  closeRegistration(): void {
    if (!this.isSaving()) this.isModalOpen.set(false);
  }

  submitRegistration(): void {
    if (this.employeeForm.invalid) {
      this.employeeForm.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);
    this.formError.set('');
    const formValue = this.employeeForm.getRawValue();
    const payload: RegisterEmployeeRequest = {
      name: formValue.name ?? '',
      role: formValue.role ?? '',
      noHp: formValue.noHp ?? '',
      dob: formValue.dob ?? '',
      password: formValue.password ?? '',
      branchId: formValue.branchId as number,
      villageId: formValue.villageId as number,
      address: formValue.address ?? '',
    };

    this.userServices
      .registerEmployee(payload)
      .subscribe({
        next: (response) => {
          this.isSaving.set(false);
          if (!response.success) {
            this.formError.set(response.message || 'Registration failed. Please try again.');
            return;
          }
          this.successMessage.set(response.message || 'Registration successful');
          this.isSuccessOpen.set(true);
        },
        error: (error: { error?: { message?: string } }) => {
          this.isSaving.set(false);
          this.formError.set(error.error?.message || 'Registration failed. Please try again.');
        },
      });
  }

  closeSuccess(): void {
    this.isSuccessOpen.set(false);
    this.isModalOpen.set(false);
    this.resetRegistrationForm();
    this.loadUsers();
  }

  sanitizePhoneNumber(event: Event): void {
    const input = event.target as HTMLInputElement;
    const numbersOnly = input.value.replace(/\D/g, '');
    if (input.value !== numbersOnly) input.value = numbersOnly;
    this.employeeForm.controls.noHp.setValue(numbersOnly, { emitEvent: false });
  }

  loadUsers(search = this.searchTerm().trim(), role = this.roleFilter()): void {
    this.isLoading.set(true);

    this.userServices.getData(this.pageIndex(), this.pageSize, search, role).subscribe({
      next: (response) => {
        const page = response.data;

        this.users.set(page?.content ?? []);
        this.totalElements.set(page?.totalElements ?? 0);
        this.totalPages.set(page?.totalPages ?? 0);
        this.isLoading.set(false);
      },
      error: () => {
        this.users.set([]);
        this.totalElements.set(0);
        this.totalPages.set(0);
        this.isLoading.set(false);
      },
    });
  }

  onSearch(value: string): void {
    this.searchTerm.set(value);
    this.searchInput$.next(value.trim());
  }

  onRoleFilterChange(value: string | null): void {
    this.roleFilter.set(value ?? '');
    this.pageIndex.set(0);
    this.loadUsers();
  }

  roleClass(role: string | null): string {
    return role ? `role-${role.toLowerCase().replace(/[^a-z0-9]+/g, '-')}` : '';
  }

  removeUser(identity: string | null): void {
    if (!identity) return;
    if (this.deletingIdentity()) return;
    this.deletingIdentity.set(identity);
    this.userServices.deleteRegistration(identity).subscribe({
      next: (response) => {
        if (response.success) {
          this.users.update((users) => users.filter((user) => user.identity !== identity));
          this.totalElements.update((total) => Math.max(0, total - 1));
        } else {
          this.formError.set(response.message || 'Failed to delete employee.');
        }
        this.deletingIdentity.set(null);
      },
      error: () => {
        this.formError.set('Failed to delete employee.');
        this.deletingIdentity.set(null);
      },
    });
  }

  previousPage(): void {
    if (this.pageIndex() === 0) {
      return;
    }

    this.pageIndex.update((page) => page - 1);
    this.loadUsers(this.searchTerm().trim());
  }

  nextPage(): void {
    if (this.pageIndex() >= this.totalPages() - 1) {
      return;
    }

    this.pageIndex.update((page) => page + 1);
    this.loadUsers(this.searchTerm().trim());
  }

  goToPage(page: number): void {
    if (page === this.pageIndex() || page < 0 || page >= this.totalPages()) return;
    this.pageIndex.set(page);
    this.loadUsers(this.searchTerm().trim());
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
