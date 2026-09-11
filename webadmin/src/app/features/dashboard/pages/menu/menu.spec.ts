import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { MenuPage } from './menu';
import { MenuServices } from '../../services/menu-services';
import { Menu } from '../../models/role.model';

const menus: Menu[] = [
  { id: 1, url: '/dashboard/user', name: 'User', logo: 'person', createdBy: 'superadmin', createdAt: '2026-01-01' },
  { id: 2, url: '/bm/approval', name: 'Approval (BM)', logo: 'fact_check', createdBy: 'superadmin', createdAt: '2026-01-01' }
];

describe('MenuPage', () => {
  let fixture: ComponentFixture<MenuPage>;
  let component: MenuPage;
  let menuServices: jasmine.SpyObj<MenuServices>;

  beforeEach(async () => {
    menuServices = jasmine.createSpyObj<MenuServices>('MenuServices', ['getAllMenus', 'updateMenu', 'deleteMenu', 'saveMenu']);
    menuServices.getAllMenus.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: menus }));

    await TestBed.configureTestingModule({
      imports: [MenuPage],
      providers: [{ provide: MenuServices, useValue: menuServices }]
    }).compileComponents();

    fixture = TestBed.createComponent(MenuPage);
    component = fixture.componentInstance;
  });

  it('[positive] loads menus on init and renders one row per menu', () => {
    fixture.detectChanges();

    expect(component.menus()).toEqual(menus);
    expect(fixture.nativeElement.querySelectorAll('tbody tr').length).toBe(menus.length);
  });

  it('[negative] shows an error and an empty list when loading menus fails', () => {
    menuServices.getAllMenus.and.returnValue(of({ success: false, statusCode: 500, message: 'Failed to load menus.', data: null as any }));

    fixture.detectChanges();

    expect(component.menus()).toEqual([]);
    expect(component.errorMessage()).toBe('Failed to load menus.');
  });

  it('[negative] a transport error also sets a fallback message', () => {
    menuServices.getAllMenus.and.returnValue(throwError(() => new Error('boom')));

    fixture.detectChanges();

    expect(component.errorMessage()).toBe('Failed to load menus.');
  });

  it('[positive] filteredMenus narrows by name (case-insensitive)', () => {
    fixture.detectChanges();

    component.onSearch('approval');

    expect(component.filteredMenus()).toEqual([menus[1]]);
  });

  describe('saveNewMenu', () => {
    it('[positive] creates the menu with name/url/logo and reloads', () => {
      fixture.detectChanges();
      menuServices.saveMenu.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: null }));
      menuServices.getAllMenus.calls.reset();

      component.openNewMenu();
      component.newMenuName.set('Inquiry (BM)');
      component.newMenuUrl.set('/bm/inquiry');
      component.newMenuLogo.set('manage_search');
      component.saveNewMenu();

      expect(menuServices.saveMenu).toHaveBeenCalledWith('Inquiry (BM)', '/bm/inquiry', 'manage_search');
      expect(component.createMenuModalOpen).toBeFalse();
      expect(menuServices.getAllMenus).toHaveBeenCalled();
    });

    it('[negative] does not call the API when name or url is blank', () => {
      fixture.detectChanges();

      component.openNewMenu();
      component.newMenuName.set('Inquiry (BM)');
      component.newMenuUrl.set('   ');
      component.saveNewMenu();

      expect(menuServices.saveMenu).not.toHaveBeenCalled();
    });

    it('[negative] surfaces the backend error on duplicate URL', () => {
      fixture.detectChanges();
      menuServices.saveMenu.and.returnValue(of({ success: false, statusCode: 409, message: 'URL already used', data: null }));

      component.openNewMenu();
      component.newMenuName.set('Approval');
      component.newMenuUrl.set('/approval');
      component.saveNewMenu();

      expect(component.errorMessage()).toBe('URL already used');
      expect(component.createMenuModalOpen).toBeTrue();
    });
  });

  describe('saveEdit', () => {
    it('[positive] updates the menu name in place', () => {
      fixture.detectChanges();
      menuServices.updateMenu.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: null }));

      component.openEdit(menus[0]);
      component.editMenuName.set('User Management');
      component.saveEdit();

      expect(menuServices.updateMenu).toHaveBeenCalledWith(1, 'User Management', '/dashboard/user', 'person');
      expect(component.menus().find((m) => m.id === 1)?.name).toBe('User Management');
      expect(component.editMenuModalOpen).toBeFalse();
    });

    it('[negative] keeps the modal open and shows the backend error on failure', () => {
      fixture.detectChanges();
      menuServices.updateMenu.and.returnValue(of({ success: false, statusCode: 500, message: 'Failed to update menu.', data: null }));

      component.openEdit(menus[0]);
      component.editMenuName.set('User Management');
      component.saveEdit();

      expect(component.errorMessage()).toBe('Failed to update menu.');
      expect(component.editMenuModalOpen).toBeTrue();
    });
  });
});
