import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Menu } from '../../models/role.model';
import { MenuServices } from '../../services/menu-services';
import { MatIcon } from '@angular/material/icon';
import { TableComponent } from '../../../../shared/components/table/table';
import { TableCellDirective } from '../../../../shared/components/table/table-cell.directive';
import { TableColumn } from '../../../../shared/components/table/table-column';
import { ModalComponent } from '../../../../shared/components/modal/modal';

@Component({
  selector: 'app-menu',
  standalone: true,
  imports: [FormsModule, MatIcon, TableComponent, TableCellDirective, ModalComponent],
  templateUrl: './menu.html',
  styleUrl: './menu.css',
})
export class MenuPage implements OnInit {
  private readonly menuServices = inject(MenuServices);

  readonly menus = signal<Menu[]>([]);
  readonly searchTerm = signal('');
  readonly isLoading = signal(false);
  readonly errorMessage = signal('');
  readonly editingMenu = signal<Menu | null>(null);
  readonly createMenu = signal(false);
  readonly editMenuName = signal('');
  readonly editMenuUrl = signal('');
  readonly editMenuLogo = signal('');
  readonly newMenuName = signal('');
  readonly newMenuUrl = signal('');
  readonly newMenuLogo = signal('');
  readonly isSaving = signal(false);
  readonly filteredMenus = computed(() => {
    const search = this.searchTerm().trim().toLowerCase();
    if (!search) return this.menus();
    return this.menus().filter((item) => (item.name ?? '').toLowerCase().includes(search));
  });

  readonly columns: TableColumn<Menu>[] = [
    { key: 'id', header: 'ID', cellClass: 'col-mono', width: '8%' },
    { key: 'url', header: 'URL', width: '18%' },
    { key: 'name', header: 'Name', width: '16%' },
    { key: 'logo', header: 'Logo', width: '10%' },
    { key: 'createdBy', header: 'Created By', width: '16%' },
    { key: 'createdAt', header: 'Created At', width: '19%' },
    { key: 'actions', header: 'Actions', headerClass: 'col-center', cellClass: 'col-center', width: '7%' },
  ];

  readonly trackByMenuId = (row: Menu) => row.id;

  get editMenuModalOpen(): boolean {
    return this.editingMenu() !== null;
  }

  set editMenuModalOpen(value: boolean) {
    if (!value) this.closeEdit();
  }

  get createMenuModalOpen(): boolean {
    return this.createMenu();
  }

  set createMenuModalOpen(value: boolean) {
    if (!value) this.closeNewMenu();
  }

  ngOnInit(): void {
    this.loadMenus();
  }

  loadMenus(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.menuServices.getAllMenus().subscribe({
      next: (response) => {
        this.menus.set(response.success ? response.data ?? [] : []);
        if (!response.success) this.errorMessage.set(response.message || 'Failed to load menus.');
        this.isLoading.set(false);
      },
      error: () => {
        this.menus.set([]);
        this.errorMessage.set('Failed to load menus.');
        this.isLoading.set(false);
      },
    });
  }

  onSearch(value: string): void {
    this.searchTerm.set(value);
  }

  openEdit(menu: Menu): void {
    this.editingMenu.set(menu);
    this.editMenuName.set(menu.name ?? '');
    this.editMenuUrl.set(menu.url ?? '');
    this.editMenuLogo.set(menu.logo ?? '');
  }

  closeEdit(): void {
    if (!this.isSaving()) this.editingMenu.set(null);
  }

  openNewMenu(): void {
    this.createMenu.set(true);
    this.newMenuName.set('');
    this.newMenuUrl.set('');
    this.newMenuLogo.set('');
  }

  closeNewMenu(): void {
    if (!this.isSaving()) this.createMenu.set(false);
  }

  saveNewMenu(): void {
    const name = this.newMenuName().trim();
    const url = this.newMenuUrl().trim();
    const logo = this.newMenuLogo().trim();
    if (!name || !url || this.isSaving()) return;

    this.isSaving.set(true);
    this.menuServices.saveMenu(name, url, logo).subscribe({
      next: (response) => {
        if (response.success) {
          this.loadMenus();
          this.createMenu.set(false);
        } else {
          this.errorMessage.set(response.message || 'Failed to create menu.');
        }
        this.isSaving.set(false);
      },
      error: () => {
        this.errorMessage.set('Failed to create menu.');
        this.isSaving.set(false);
      },
    });
  }

  saveEdit(): void {
    const menu = this.editingMenu();
    const name = this.editMenuName().trim();
    const url = this.editMenuUrl().trim();
    const logo = this.editMenuLogo().trim();
    if (!menu || !name || !url || this.isSaving()) return;

    this.isSaving.set(true);
    this.menuServices.updateMenu(menu.id, name, url, logo).subscribe({
      next: (response) => {
        if (response.success) {
          this.menus.update((menus) =>
            menus.map((item) => item.id === menu.id ? { ...item, name, url, logo } : item),
          );
          this.editingMenu.set(null);
        } else {
          this.errorMessage.set(response.message || 'Failed to update menu.');
        }
        this.isSaving.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err.message || 'Failed to update menu.');
        this.isSaving.set(false);
      },
    });
  }
}
