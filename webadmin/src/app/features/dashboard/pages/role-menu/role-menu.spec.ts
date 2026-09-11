import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { RoleMenuPage } from './role-menu';
import { RoleServices } from '../../services/role-services';
import { MenuServices } from '../../services/menu-services';
import { RoleMenuService } from '../../services/role-menu-services';
import { Role, Menu, RoleMenu } from '../../models/role.model';

const role: Role = { id: 2, role: 'BM' };
const menus: Menu[] = [
  { id: 1, url: '/bm/approval', name: 'Approval (BM)', logo: 'fact_check', createdBy: 'x', createdAt: 'x' },
  { id: 2, url: '/bm/inquiry', name: 'Inquiry (BM)', logo: 'manage_search', createdBy: 'x', createdAt: 'x' }
];
const assignedRoleMenus: RoleMenu[] = [
  { id: 6, roleId: 2, role: 'BM', menuId: 1, menu: 'Approval (BM)', url: '/bm/approval', logo: 'fact_check', assignedBy: 'x', assignedAt: 'x' }
];

describe('RoleMenuPage', () => {
  let fixture: ComponentFixture<RoleMenuPage>;
  let component: RoleMenuPage;
  let roleServices: jasmine.SpyObj<RoleServices>;
  let menuServices: jasmine.SpyObj<MenuServices>;
  let roleMenuService: jasmine.SpyObj<RoleMenuService>;

  beforeEach(async () => {
    roleServices = jasmine.createSpyObj<RoleServices>('RoleServices', ['getAllRoles']);
    menuServices = jasmine.createSpyObj<MenuServices>('MenuServices', ['getAllMenus']);
    roleMenuService = jasmine.createSpyObj<RoleMenuService>('RoleMenuService', ['getAllRoleMenus', 'addRoleMenu', 'deleteRoleMenu']);

    roleServices.getAllRoles.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [role] }));
    menuServices.getAllMenus.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: menus }));
    roleMenuService.getAllRoleMenus.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: assignedRoleMenus }));

    await TestBed.configureTestingModule({
      imports: [RoleMenuPage],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '2' }) } } },
        { provide: RoleServices, useValue: roleServices },
        { provide: MenuServices, useValue: menuServices },
        { provide: RoleMenuService, useValue: roleMenuService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RoleMenuPage);
    component = fixture.componentInstance;
  });

  it('[positive] loads the role, all menus, and marks the currently-assigned ones', () => {
    fixture.detectChanges();

    expect(component.role()).toEqual(role);
    expect(component.menus()).toEqual(menus);
    expect(component.isAssigned(1)).toBeTrue();
    expect(component.isAssigned(2)).toBeFalse();
    expect(roleMenuService.getAllRoleMenus).toHaveBeenCalledWith({ role: 'BM' });
  });

  it('[negative] shows "Role not found." when the id in the URL does not match any role', () => {
    roleServices.getAllRoles.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [{ id: 999, role: 'GHOST' }] }));

    fixture.detectChanges();

    expect(component.errorMessage()).toBe('Role not found.');
    expect(component.isLoading()).toBeFalse();
  });

  it('[negative] surfaces an error when loading the role list itself fails', () => {
    roleServices.getAllRoles.and.returnValue(throwError(() => new Error('boom')));

    fixture.detectChanges();

    expect(component.errorMessage()).toBe('Failed to load role.');
  });

  describe('toggleMenu', () => {
    it('[positive] checking an unassigned menu calls addRoleMenu and reloads assignments', () => {
      fixture.detectChanges();
      roleMenuService.addRoleMenu.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: null }));
      roleMenuService.getAllRoleMenus.calls.reset();

      component.toggleMenu(menus[1], { target: { checked: true } } as unknown as Event);

      expect(roleMenuService.addRoleMenu).toHaveBeenCalledWith('BM', 'Inquiry (BM)');
      expect(roleMenuService.getAllRoleMenus).toHaveBeenCalledWith({ role: 'BM' });
      expect(component.togglingMenuId()).toBeNull();
    });

    it('[positive] unchecking an assigned menu calls deleteRoleMenu with its role-menu row id', () => {
      fixture.detectChanges();
      roleMenuService.deleteRoleMenu.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: null }));

      component.toggleMenu(menus[0], { target: { checked: false } } as unknown as Event);

      expect(roleMenuService.deleteRoleMenu).toHaveBeenCalledWith(6);
    });

    it('[negative] surfaces an error and does not change state when the toggle call fails', () => {
      fixture.detectChanges();
      roleMenuService.addRoleMenu.and.returnValue(throwError(() => new Error('boom')));

      component.toggleMenu(menus[1], { target: { checked: true } } as unknown as Event);

      expect(component.errorMessage()).toBe('Failed to update menu access.');
      expect(component.togglingMenuId()).toBeNull();
    });

    it('[negative] ignores a second toggle while one is already in flight', () => {
      fixture.detectChanges();
      roleMenuService.addRoleMenu.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: null }));
      component['togglingMenuId'].set(1);

      component.toggleMenu(menus[1], { target: { checked: true } } as unknown as Event);

      expect(roleMenuService.addRoleMenu).not.toHaveBeenCalled();
    });
  });
});
