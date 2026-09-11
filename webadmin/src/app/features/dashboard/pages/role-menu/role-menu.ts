import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatIcon } from '@angular/material/icon';
import { Role, Menu, RoleMenu } from '../../models/role.model';
import { RoleServices } from '../../services/role-services';
import { MenuServices } from '../../services/menu-services';
import { RoleMenuService } from '../../services/role-menu-services';
import { TableComponent } from '../../../../shared/components/table/table';
import { TableCellDirective } from '../../../../shared/components/table/table-cell.directive';
import { TableColumn } from '../../../../shared/components/table/table-column';

@Component({
  selector: 'app-role-menu',
  standalone: true,
  imports: [RouterLink, MatIcon, TableComponent, TableCellDirective],
  templateUrl: './role-menu.html',
  styleUrl: './role-menu.css',
})
export class RoleMenuPage implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly roleServices = inject(RoleServices);
  private readonly menuServices = inject(MenuServices);
  private readonly roleMenuService = inject(RoleMenuService);

  private readonly roleId = Number(this.route.snapshot.paramMap.get('id'));

  readonly role = signal<Role | null>(null);
  readonly menus = signal<Menu[]>([]);
  readonly roleMenus = signal<RoleMenu[]>([]);
  readonly isLoading = signal(false);
  readonly errorMessage = signal('');
  readonly togglingMenuId = signal<number | null>(null);

  readonly assignedMenuIds = computed(() => new Set(this.roleMenus().map((item) => item.menuId)));

  readonly columns: TableColumn<Menu>[] = [
    { key: 'assigned', header: 'Access', headerClass: 'col-center', cellClass: 'col-center', width: '10%' },
    { key: 'logo', header: 'Logo', width: '10%' },
    { key: 'name', header: 'Menu Name', width: '32%' },
    { key: 'url', header: 'URL', cellClass: 'col-mono', width: '48%' },
  ];

  readonly trackByMenuId = (row: Menu) => row.id;

  ngOnInit(): void {
    this.loadMenus();
    this.loadRoleAndAssignments();
  }

  isAssigned(menuId: number): boolean {
    return this.assignedMenuIds().has(menuId);
  }

  toggleMenu(menu: Menu, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    const role = this.role();
    if (!role?.role || !menu.name || this.togglingMenuId() !== null) return;

    this.togglingMenuId.set(menu.id);
    const request$ = checked
      ? this.roleMenuService.addRoleMenu(role.role, menu.name)
      : this.roleMenuService.deleteRoleMenu(
          this.roleMenus().find((item) => item.menuId === menu.id)?.id ?? 0,
        );

    request$.subscribe({
      next: (response) => {
        if (response.success) {
          this.reloadAssignments(role.role!);
        } else {
          this.errorMessage.set(response.message || 'Failed to update menu access.');
        }
        this.togglingMenuId.set(null);
      },
      error: () => {
        this.errorMessage.set('Failed to update menu access.');
        this.togglingMenuId.set(null);
      },
    });
  }

  private loadMenus(): void {
    this.menuServices.getAllMenus().subscribe({
      next: (response) => this.menus.set(response.success ? response.data ?? [] : []),
      error: () => this.menus.set([]),
    });
  }

  private loadRoleAndAssignments(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.roleServices.getAllRoles().subscribe({
      next: (response) => {
        const role = (response.data ?? []).find((item) => item.id === this.roleId) ?? null;
        this.role.set(role);
        if (!role?.role) {
          this.errorMessage.set('Role not found.');
          this.isLoading.set(false);
          return;
        }
        this.reloadAssignments(role.role);
      },
      error: () => {
        this.errorMessage.set('Failed to load role.');
        this.isLoading.set(false);
      },
    });
  }

  private reloadAssignments(roleName: string): void {
    this.roleMenuService.getAllRoleMenus({ role: roleName }).subscribe({
      next: (response) => {
        this.roleMenus.set(response.success ? response.data ?? [] : []);
        this.isLoading.set(false);
      },
      error: () => {
        this.roleMenus.set([]);
        this.errorMessage.set('Failed to load assigned menus.');
        this.isLoading.set(false);
      },
    });
  }
}
