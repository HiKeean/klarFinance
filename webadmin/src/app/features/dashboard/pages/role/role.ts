import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Role } from '../../models/role.model';
import { RoleServices } from '../../services/role-services';
import { MatIcon } from "@angular/material/icon";
import { TableComponent } from '../../../../shared/components/table/table';
import { TableCellDirective } from '../../../../shared/components/table/table-cell.directive';
import { TableColumn } from '../../../../shared/components/table/table-column';
import { ModalComponent } from '../../../../shared/components/modal/modal';

@Component({
  selector: 'app-role',
  standalone: true,
  imports: [
    FormsModule
    , MatIcon
    , TableComponent
    , TableCellDirective
    , ModalComponent
  ],
  templateUrl: './role.html',
  styleUrl: './role.css',
})
export class RolePage implements OnInit {
  private readonly roleServices = inject(RoleServices);
  private readonly router = inject(Router);

  readonly roles = signal<Role[]>([]);
  readonly searchTerm = signal('');
  readonly isLoading = signal(false);
  readonly errorMessage = signal('');
  readonly editingRole = signal<Role | null>(null);
  readonly createRole = signal<Boolean | null>(false);
  readonly editRoleName = signal('');
  readonly isSaving = signal(false);
  readonly filteredRoles = computed(() => {
    const search = this.searchTerm().trim().toLowerCase();
    if (!search) return this.roles();
    return this.roles().filter((item) => (item.role ?? '').toLowerCase().includes(search));
  });

  readonly columns: TableColumn<Role>[] = [
    { key: 'id', header: 'ID', cellClass: 'col-mono', width: '25%' },
    { key: 'role', header: 'Role', width: '65%' },
    { key: 'actions', header: 'Actions', headerClass: 'col-center', cellClass: 'col-center', width: '10%' },
  ];

  readonly trackByRoleId = (row: Role) => row.id;

  readonly rowClass = () => 'row-clickable';

  openMenuAccess(role: Role): void {
    this.router.navigate(['/dashboard/role', role.id, 'menus']);
  }

  get editRoleModalOpen(): boolean {
    return this.editingRole() !== null;
  }

  set editRoleModalOpen(value: boolean) {
    if (!value) this.closeEdit();
  }

  get createRoleModalOpen(): boolean {
    return !!this.createRole();
  }

  set createRoleModalOpen(value: boolean) {
    if (!value) this.closeNewRole();
  }

  ngOnInit(): void {
    this.loadRoles();
  }

  loadRoles(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.roleServices.getAllRoles().subscribe({
      next: (response) => {
        this.roles.set(response.success ? response.data ?? [] : []);
        if (!response.success) this.errorMessage.set(response.message || 'Failed to load roles.');
        this.isLoading.set(false);
      },
      error: () => {
        this.roles.set([]);
        this.errorMessage.set('Failed to load roles.');
        this.isLoading.set(false);
      },
    });
  }

  onSearch(value: string): void {
    this.searchTerm.set(value);
  }

  openNewRole(): void {
    this.createRole.set(true);
    this.editRoleName.set('');
  }

  closeNewRole(): void {
    if (!this.isSaving()) this.createRole.set(false);
  }

  openEdit(role: Role): void {
    this.editingRole.set(role);
    this.editRoleName.set(role.role ?? '');
  }

  closeEdit(): void {
    if (!this.isSaving()) this.editingRole.set(null);
  }

  saveEdit(): void {
    const role = this.editingRole();
    const name = this.editRoleName().trim();
    if (!role || !name || this.isSaving()) return;

    this.isSaving.set(true);
    this.roleServices.updateRole(role.id, name).subscribe({
      next: (response) => {
        if (response.success) {
          this.roles.update((roles) =>
            roles.map((item) => item.id === role.id ? { ...item, role: name } : item),
          );
          this.editingRole.set(null);
        } else {
          this.errorMessage.set(response.message || 'Failed to update role.');
        }
        this.isSaving.set(false);
      },
      error: () => {
        this.errorMessage.set('Failed to update role.');
        this.isSaving.set(false);
      },
    });
  }

  saveNewRole():void{
    const name = this.editRoleName().trim();
    if (!name || this.isSaving()) return;

    this.isSaving.set(true);
    this.roleServices.saveRole(name).subscribe({
      next: (response) => {
        if (response.success) {
          this.loadRoles();
          this.createRole.set(false);
        } else {
          this.errorMessage.set(response.message || 'Failed to create role.');
        }
        this.isSaving.set(false);
      },
      error: () => {
        this.errorMessage.set('Failed to create role.');
        this.isSaving.set(false);
      },
    });
  }

  deleteRole(role: Role): void {
    if (!role || this.isSaving()) return;

    if (!confirm(`Are you sure you want to delete the role "${role.role}"?`)) return;

    this.isSaving.set(true);
    this.roleServices.deleteRole(role.id).subscribe({
      next: (response) => {
        if (response.success) {
          this.roles.update((roles) => roles.filter((item) => item.id !== role.id));
        } else {
          this.errorMessage.set(response.message || 'Failed to delete role.');
        }
        this.isSaving.set(false);
      },
      error: () => {
        this.errorMessage.set('Failed to delete role.');
        this.isSaving.set(false);
      },
    });
  }

}
