import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { RolePage } from './role';
import { RoleServices } from '../../services/role-services';
import { Role } from '../../models/role.model';

const roles: Role[] = [{ id: 1, role: 'SUPERADMIN' }, { id: 2, role: 'BM' }];

describe('RolePage', () => {
  let fixture: ComponentFixture<RolePage>;
  let component: RolePage;
  let roleServices: jasmine.SpyObj<RoleServices>;
  let router: Router;

  beforeEach(async () => {
    roleServices = jasmine.createSpyObj<RoleServices>('RoleServices', ['getAllRoles', 'updateRole', 'deleteRole', 'saveRole']);
    roleServices.getAllRoles.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: roles }));

    await TestBed.configureTestingModule({
      imports: [RolePage],
      providers: [provideRouter([]), { provide: RoleServices, useValue: roleServices }]
    }).compileComponents();

    fixture = TestBed.createComponent(RolePage);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
  });

  it('[positive] loads roles on init and renders one row per role', () => {
    fixture.detectChanges();

    expect(component.roles()).toEqual(roles);
    const rows = fixture.nativeElement.querySelectorAll('tbody tr');
    expect(rows.length).toBe(roles.length);
  });

  it('[negative] shows an error and an empty list when loading roles fails', () => {
    roleServices.getAllRoles.and.returnValue(of({ success: false, statusCode: 500, message: 'Failed to load roles.', data: null as any }));

    fixture.detectChanges();

    expect(component.roles()).toEqual([]);
    expect(component.errorMessage()).toBe('Failed to load roles.');
  });

  it('[negative] a transport error also clears the list and sets a fallback message', () => {
    roleServices.getAllRoles.and.returnValue(throwError(() => new Error('boom')));

    fixture.detectChanges();

    expect(component.roles()).toEqual([]);
    expect(component.errorMessage()).toBe('Failed to load roles.');
  });

  it('[positive] filteredRoles narrows by the search term (case-insensitive)', () => {
    fixture.detectChanges();

    component.onSearch('bm');

    expect(component.filteredRoles()).toEqual([{ id: 2, role: 'BM' }]);
  });

  it('[positive] openMenuAccess navigates to the role-menu page for that role', () => {
    fixture.detectChanges();

    component.openMenuAccess(roles[1]);

    expect(router.navigate).toHaveBeenCalledWith(['/dashboard/role', 2, 'menus']);
  });

  describe('saveNewRole', () => {
    it('[positive] creates the role and reloads the list', () => {
      fixture.detectChanges();
      roleServices.saveRole.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: null }));
      roleServices.getAllRoles.calls.reset();

      component.openNewRole();
      component.editRoleName.set('FINANCE');
      component.saveNewRole();

      expect(roleServices.saveRole).toHaveBeenCalledWith('FINANCE');
      expect(component.createRoleModalOpen).toBeFalse();
      expect(roleServices.getAllRoles).toHaveBeenCalled();
    });

    it('[negative] surfaces the backend error and keeps the modal open on duplicate role', () => {
      fixture.detectChanges();
      roleServices.saveRole.and.returnValue(of({ success: false, statusCode: 409, message: 'Role already exists', data: null }));

      component.openNewRole();
      component.editRoleName.set('BM');
      component.saveNewRole();

      expect(component.errorMessage()).toBe('Role already exists');
      expect(component.createRoleModalOpen).toBeTrue();
    });

    it('[negative] does nothing when the name is blank', () => {
      fixture.detectChanges();

      component.openNewRole();
      component.editRoleName.set('   ');
      component.saveNewRole();

      expect(roleServices.saveRole).not.toHaveBeenCalled();
    });
  });

  describe('saveEdit', () => {
    it('[positive] updates the role in place', () => {
      fixture.detectChanges();
      roleServices.updateRole.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: null }));

      component.openEdit(roles[1]);
      component.editRoleName.set('BM_SENIOR');
      component.saveEdit();

      expect(roleServices.updateRole).toHaveBeenCalledWith(2, 'BM_SENIOR');
      expect(component.roles().find((r) => r.id === 2)?.role).toBe('BM_SENIOR');
      expect(component.editRoleModalOpen).toBeFalse();
    });

    it('[negative] keeps the modal open and shows the error on failure', () => {
      fixture.detectChanges();
      roleServices.updateRole.and.returnValue(of({ success: false, statusCode: 500, message: 'Failed to update role.', data: null }));

      component.openEdit(roles[1]);
      component.editRoleName.set('BM_SENIOR');
      component.saveEdit();

      expect(component.errorMessage()).toBe('Failed to update role.');
      expect(component.editRoleModalOpen).toBeTrue();
    });
  });

  describe('deleteRole', () => {
    it('[positive] removes the role after confirmation', () => {
      fixture.detectChanges();
      spyOn(window, 'confirm').and.returnValue(true);
      roleServices.deleteRole.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: null }));

      component.deleteRole(roles[1]);

      expect(component.roles()).toEqual([roles[0]]);
    });

    it('[negative] does not call the API when the user cancels the confirmation', () => {
      fixture.detectChanges();
      spyOn(window, 'confirm').and.returnValue(false);

      component.deleteRole(roles[1]);

      expect(roleServices.deleteRole).not.toHaveBeenCalled();
      expect(component.roles()).toEqual(roles);
    });
  });
});
