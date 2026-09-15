import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideMockStore } from '@ngrx/store/testing';
import { Store } from '@ngrx/store';
import { of, throwError } from 'rxjs';

import { User } from './user';
import { UserServices } from '../../services/user-services';
import { Dbo } from '../../../../core/services/dbo/dbo';
import * as RegistrationActions from './user-registration.actions';
import { User as UserModel } from '../../models/user.model';

/**
 * Covers the employee-registration modal's cascading branch/province/regency/district/
 * village loading (openRegistration, onProvinceChanged/onRegencyChanged/onDistrictChanged),
 * plus the smaller helpers (sanitizePhoneNumber, role filter, removeUser's inline-message
 * branch, pagination) that user.spec.ts's submit-focused flow never triggers. Kept as its
 * own file rather than growing user.spec.ts, per this task's "only add new specs" rule.
 */
const sampleUser: UserModel = {
  identity: '20260001',
  name: 'Budi',
  role: 'BM',
  noHp: '0812345678',
  branch: { branchCode: 1000, name: 'Head Office' },
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
  deletedAt: null
};

describe('User (registration modal + list helpers)', () => {
  let fixture: ComponentFixture<User>;
  let component: User;
  let userServices: jasmine.SpyObj<UserServices>;
  let dbo: jasmine.SpyObj<Dbo>;
  let store: Store;

  const initialState = {
    userRegistration: {
      branches: [], provinces: [], regencies: [], districts: [], villages: [],
      queries: { branch: '', province: '', regency: '', district: '', village: '' },
      loading: false, loaded: false, error: ''
    }
  };

  beforeEach(async () => {
    userServices = jasmine.createSpyObj<UserServices>('UserServices', ['getData', 'registerEmployee', 'deleteRegistration']);
    dbo = jasmine.createSpyObj<Dbo>('Dbo', ['getAllBranches', 'getAllProvinces', 'getAllRegencies', 'getAllDistricts', 'getAllVillages']);
    userServices.getData.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: { content: [], totalElements: 0, totalPages: 0, currentPage: 0, pageSize: 10 } }));

    await TestBed.configureTestingModule({
      imports: [User],
      providers: [
        provideMockStore({ initialState }),
        { provide: UserServices, useValue: userServices },
        { provide: Dbo, useValue: dbo }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(User);
    component = fixture.componentInstance;
    store = TestBed.inject(Store);
    fixture.detectChanges(); // ngOnInit: wires up the cascading valueChanges subscriptions
  });

  describe('openRegistration', () => {
    it('[positive] loads branches and provinces, dispatches setBranches, and opens the modal', () => {
      spyOn(store, 'dispatch');
      dbo.getAllBranches.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [{ branchCode: 1, name: 'HO' }] }));
      dbo.getAllProvinces.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [{ id: 1, name: 'Jawa Barat' }] }));

      component.openRegistration();

      expect(component.isModalOpen()).toBeTrue();
      expect(component.formError()).toBe('');
      expect(store.dispatch).toHaveBeenCalledWith(RegistrationActions.setBranches({ items: [{ branchCode: 1, name: 'HO' }] as any }));
      expect(component.provinceOptions()).toEqual([{ label: 'Jawa Barat', value: 1 }]);
    });

    it('[negative] sets formError when branches fail to load', () => {
      dbo.getAllBranches.and.returnValue(throwError(() => new Error('down')));
      dbo.getAllProvinces.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [] }));

      component.openRegistration();

      expect(component.formError()).toBe('Unable to load branch data.');
    });

    it('[negative] sets formError when provinces fail to load', () => {
      dbo.getAllBranches.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [] }));
      dbo.getAllProvinces.and.returnValue(throwError(() => new Error('down')));

      component.openRegistration();

      expect(component.formError()).toBe('Unable to load province data.');
    });
  });

  describe('cascading location selects', () => {
    beforeEach(() => {
      dbo.getAllBranches.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [] }));
      dbo.getAllProvinces.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [] }));
    });

    it('[positive] selecting a province enables + loads regencies', () => {
      dbo.getAllRegencies.and.returnValue(of({
        success: true, statusCode: 200, message: 'OK',
        data: [{ id: 5, name: 'Bandung', province: { id: 1, name: 'Jawa Barat' } }]
      }));

      component.employeeForm.controls.provinceId.setValue(1);

      expect(component.regencyOptions()).toEqual([{ label: 'Bandung', value: 5 }]);
      expect(component.employeeForm.controls.regencyId.disabled).toBeFalse();
    });

    it('[negative] regency load error sets formError', () => {
      dbo.getAllRegencies.and.returnValue(throwError(() => new Error('down')));

      component.employeeForm.controls.provinceId.setValue(1);

      expect(component.formError()).toBe('Unable to load regency data.');
    });

    it('[positive] toList unwraps a paginated {content: [...]} response shape', () => {
      dbo.getAllRegencies.and.returnValue(of({
        success: true, statusCode: 200, message: 'OK',
        data: { content: [{ id: 9, name: 'Cimahi' }] } as any
      }));

      component.employeeForm.controls.provinceId.setValue(1);

      expect(component.regencyOptions()).toEqual([{ label: 'Cimahi', value: 9 }]);
    });

    it('[negative] toList falls back to an empty list for an unrecognized data shape', () => {
      dbo.getAllRegencies.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: {} as any }));

      component.employeeForm.controls.provinceId.setValue(1);

      expect(component.regencyOptions()).toEqual([]);
    });

    it('[negative] clearing the province disables + resets regency/district/village without calling dbo', () => {
      dbo.getAllRegencies.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [] }));
      component.employeeForm.controls.provinceId.setValue(1);
      dbo.getAllRegencies.calls.reset();

      component.employeeForm.controls.provinceId.setValue(null);

      expect(dbo.getAllRegencies).not.toHaveBeenCalled();
      expect(component.regencyOptions()).toEqual([]);
      expect(component.employeeForm.controls.regencyId.disabled).toBeTrue();
      expect(component.employeeForm.controls.districtId.disabled).toBeTrue();
      expect(component.employeeForm.controls.villageId.disabled).toBeTrue();
    });

    it('[positive] selecting a regency enables + loads districts', () => {
      dbo.getAllDistricts.and.returnValue(of({
        success: true, statusCode: 200, message: 'OK',
        data: [{ id: 7, name: 'Cimahi Utara', regency: { id: 5, name: 'Cimahi', province: { id: 1, name: 'Jawa Barat' } } }]
      }));

      component.employeeForm.controls.regencyId.setValue(5);

      expect(component.districtOptions()).toEqual([{ label: 'Cimahi Utara', value: 7 }]);
      expect(component.employeeForm.controls.districtId.disabled).toBeFalse();
    });

    it('[negative] district load error sets formError', () => {
      dbo.getAllDistricts.and.returnValue(throwError(() => new Error('down')));

      component.employeeForm.controls.regencyId.setValue(5);

      expect(component.formError()).toBe('Unable to load district data.');
    });

    it('[negative] clearing the regency disables + resets district/village without calling dbo', () => {
      dbo.getAllDistricts.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [] }));
      component.employeeForm.controls.regencyId.setValue(5);
      dbo.getAllDistricts.calls.reset();

      component.employeeForm.controls.regencyId.setValue(null);

      expect(dbo.getAllDistricts).not.toHaveBeenCalled();
      expect(component.districtOptions()).toEqual([]);
      expect(component.employeeForm.controls.districtId.disabled).toBeTrue();
      expect(component.employeeForm.controls.villageId.disabled).toBeTrue();
    });

    it('[positive] selecting a district enables + loads villages', () => {
      dbo.getAllVillages.and.returnValue(of({
        success: true, statusCode: 200, message: 'OK',
        data: [{ id: 3, name: 'Desa A', district: null }]
      }));

      component.employeeForm.controls.districtId.setValue(7);

      expect(component.villageOptions()).toEqual([{ label: 'Desa A', value: 3 }]);
      expect(component.employeeForm.controls.villageId.disabled).toBeFalse();
    });

    it('[negative] village load error sets formError', () => {
      dbo.getAllVillages.and.returnValue(throwError(() => new Error('down')));

      component.employeeForm.controls.districtId.setValue(7);

      expect(component.formError()).toBe('Unable to load village data.');
    });

    it('[negative] clearing the district disables + resets village without calling dbo', () => {
      dbo.getAllVillages.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [] }));
      component.employeeForm.controls.districtId.setValue(7);
      dbo.getAllVillages.calls.reset();

      component.employeeForm.controls.districtId.setValue(null);

      expect(dbo.getAllVillages).not.toHaveBeenCalled();
      expect(component.villageOptions()).toEqual([]);
      expect(component.employeeForm.controls.villageId.disabled).toBeTrue();
    });
  });

  describe('closeSuccess / resetRegistrationForm', () => {
    it('[positive] closes both modals, resets the form, and reloads the user list', () => {
      component.regencyOptions.set([{ label: 'X', value: 1 }]);
      userServices.getData.calls.reset();

      component.closeSuccess();

      expect(component.isSuccessOpen()).toBeFalse();
      expect(component.isModalOpen()).toBeFalse();
      expect(component.regencyOptions()).toEqual([]);
      expect(component.districtOptions()).toEqual([]);
      expect(component.villageOptions()).toEqual([]);
      expect(component.employeeForm.controls.regencyId.disabled).toBeTrue();
      expect(userServices.getData).toHaveBeenCalled();
    });
  });

  describe('closeRegistration', () => {
    it('[negative] does nothing while a save is in progress', () => {
      component.isModalOpen.set(true);
      component.isSaving.set(true);

      component.closeRegistration();

      expect(component.isModalOpen()).toBeTrue();
    });

    it('[positive] closes the modal when not saving', () => {
      component.isModalOpen.set(true);
      component.isSaving.set(false);

      component.closeRegistration();

      expect(component.isModalOpen()).toBeFalse();
    });
  });

  describe('sanitizePhoneNumber', () => {
    it('[positive] strips non-digit characters from the input and syncs the noHp control', () => {
      const input = { value: '08-12 34ab' } as HTMLInputElement;
      component.sanitizePhoneNumber({ target: input } as unknown as Event);

      expect(input.value).toBe('081234');
      expect(component.employeeForm.controls.noHp.value).toBe('081234');
    });

    it('[negative] leaves an already-numeric input untouched', () => {
      const input = { value: '081234' } as HTMLInputElement;
      component.sanitizePhoneNumber({ target: input } as unknown as Event);

      expect(input.value).toBe('081234');
    });
  });

  describe('onRoleFilterChange', () => {
    it('[positive] sets the filter, resets to page 0, and reloads', () => {
      component.pageIndex.set(2);
      userServices.getData.calls.reset();

      component.onRoleFilterChange('CHECKER');

      expect(component.roleFilter()).toBe('CHECKER');
      expect(component.pageIndex()).toBe(0);
      expect(userServices.getData).toHaveBeenCalledWith(0, 10, '', 'CHECKER');
    });

    it('[negative] a null value clears the filter', () => {
      component.onRoleFilterChange(null);
      expect(component.roleFilter()).toBe('');
    });
  });

  describe('removeUser', () => {
    it('[negative] surfaces the backend message when deletion responds with success:false', () => {
      userServices.deleteRegistration.and.returnValue(of({ success: false, statusCode: 409, message: 'Cannot delete: active loans', data: null as any }));
      component.users.set([sampleUser]);

      component.removeUser(sampleUser.identity);

      expect(component.formError()).toBe('Cannot delete: active loans');
      expect(component.users()).toEqual([sampleUser]);
      expect(component.deletingIdentity()).toBeNull();
    });
  });

  describe('pagination', () => {
    it('[positive] previousPage moves back a page and reloads when not on the first page', () => {
      component.pageIndex.set(2);
      userServices.getData.calls.reset();

      component.previousPage();

      expect(component.pageIndex()).toBe(1);
      expect(userServices.getData).toHaveBeenCalledWith(1, 10, '', '');
    });

    it('[positive] nextPage advances a page and reloads when more pages remain', () => {
      component.pageIndex.set(0);
      component.totalPages.set(3);
      userServices.getData.calls.reset();

      component.nextPage();

      expect(component.pageIndex()).toBe(1);
      expect(userServices.getData).toHaveBeenCalledWith(1, 10, '', '');
    });

    describe('visiblePages', () => {
      it('[positive] returns every page when totalPages <= 5', () => {
        component.totalPages.set(4);
        expect(component.visiblePages()).toEqual([0, 1, 2, 3]);
      });

      it('[positive] anchors to the start window near the beginning', () => {
        component.totalPages.set(10);
        component.pageIndex.set(1);
        expect(component.visiblePages()).toEqual([0, 1, 2, 3, 4]);
      });

      it('[positive] anchors to the end window near the end', () => {
        component.totalPages.set(10);
        component.pageIndex.set(8);
        expect(component.visiblePages()).toEqual([5, 6, 7, 8, 9]);
      });

      it('[positive] centers the window otherwise', () => {
        component.totalPages.set(10);
        component.pageIndex.set(5);
        expect(component.visiblePages()).toEqual([3, 4, 5, 6, 7]);
      });
    });
  });
});
