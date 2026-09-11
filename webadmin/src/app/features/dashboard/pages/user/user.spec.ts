import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideMockStore } from '@ngrx/store/testing';
import { of, throwError } from 'rxjs';

import { User } from './user';
import { UserServices } from '../../services/user-services';
import { Dbo } from '../../../../core/services/dbo/dbo';
import { User as UserModel, UserPageResponse } from '../../models/user.model';
import { RegisterEmployeeRequest } from '../../models/registration.model';

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

const samplePage: UserPageResponse = { content: [sampleUser], totalElements: 1, totalPages: 1, currentPage: 0, pageSize: 10 };

const validPayload: RegisterEmployeeRequest = {
  name: 'Budi',
  role: 'BM',
  noHp: '081234',
  dob: '2000-01-01',
  password: 'secret1',
  branchId: 1000,
  villageId: 1,
  address: 'Jl. A'
};

describe('User (dashboard employee registration page)', () => {
  let fixture: ComponentFixture<User>;
  let component: User;
  let userServices: jasmine.SpyObj<UserServices>;
  let dbo: jasmine.SpyObj<Dbo>;

  beforeEach(async () => {
    userServices = jasmine.createSpyObj<UserServices>('UserServices', ['getData', 'registerEmployee', 'deleteRegistration']);
    dbo = jasmine.createSpyObj<Dbo>('Dbo', ['getAllBranches', 'getAllProvinces', 'getAllRegencies', 'getAllDistricts', 'getAllVillages']);
    userServices.getData.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: samplePage }));
    // The form's province/regency/district controls cascade into Dbo calls on every valueChange
    // (even when the test only cares about the final submit) - default them so setValue(...) doesn't throw.
    dbo.getAllBranches.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [] }));
    dbo.getAllProvinces.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [] }));
    dbo.getAllRegencies.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [] }));
    dbo.getAllDistricts.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [] }));
    dbo.getAllVillages.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [] }));

    await TestBed.configureTestingModule({
      imports: [User],
      providers: [
        provideMockStore({ initialState: { userRegistration: { branches: [], provinces: [], regencies: [], districts: [], villages: [], queries: { branch: '', province: '', regency: '', district: '', village: '' }, loading: false, loaded: false, error: '' } } }),
        { provide: UserServices, useValue: userServices },
        { provide: Dbo, useValue: dbo }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(User);
    component = fixture.componentInstance;
  });

  it('should create and load the first page of users on init', () => {
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(userServices.getData).toHaveBeenCalledWith(0, 10, '');
    expect(component.users()).toEqual([sampleUser]);
    expect(component.totalElements()).toBe(1);
    expect(component.isLoading()).toBeFalse();
  });

  describe('negative: loading', () => {
    it('clears the list and stops loading when getData errors', () => {
      userServices.getData.and.returnValue(throwError(() => new Error('network down')));

      fixture.detectChanges();

      expect(component.users()).toEqual([]);
      expect(component.totalElements()).toBe(0);
      expect(component.isLoading()).toBeFalse();
    });
  });

  describe('search', () => {
    // This app runs zoneless (no zone.js), so fakeAsync/tick aren't available - debounceTime(400)
    // is a real RxJS timer here, exercised with a real (short) wait instead of a virtual clock.
    it('[positive] debounces search input and reloads with the trimmed term', async () => {
      fixture.detectChanges();
      userServices.getData.calls.reset();

      component.onSearch('  budi  ');
      await new Promise((resolve) => setTimeout(resolve, 200));
      expect(userServices.getData).not.toHaveBeenCalled();

      await new Promise((resolve) => setTimeout(resolve, 300));
      expect(userServices.getData).toHaveBeenCalledWith(0, 10, 'budi');
    });
  });

  describe('submitRegistration', () => {
    it('[negative] does not call the API and marks the form touched when required fields are missing', () => {
      fixture.detectChanges();

      component.submitRegistration();

      expect(userServices.registerEmployee).not.toHaveBeenCalled();
      expect(component.employeeForm.touched).toBeTrue();
    });

    it('[positive] registers the employee and opens the success dialog', () => {
      fixture.detectChanges();
      userServices.registerEmployee.and.returnValue(of({ success: true, statusCode: 200, message: 'Account berhasil diregistrasi.', data: null }));
      component.employeeForm.setValue({
        name: validPayload.name,
        role: validPayload.role,
        noHp: validPayload.noHp,
        dob: validPayload.dob,
        password: validPayload.password,
        branchId: validPayload.branchId,
        provinceId: 1,
        regencyId: 1,
        districtId: 1,
        villageId: validPayload.villageId,
        address: validPayload.address
      });

      component.submitRegistration();

      expect(userServices.registerEmployee).toHaveBeenCalledWith(validPayload);
      expect(component.isSuccessOpen()).toBeTrue();
      expect(component.isSaving()).toBeFalse();
    });

    it('[negative] surfaces the backend message and keeps the modal open when registration fails', () => {
      fixture.detectChanges();
      userServices.registerEmployee.and.returnValue(of({ success: false, statusCode: 409, message: 'Identity already exists', data: null }));
      component.employeeForm.setValue({
        name: validPayload.name,
        role: validPayload.role,
        noHp: validPayload.noHp,
        dob: validPayload.dob,
        password: validPayload.password,
        branchId: validPayload.branchId,
        provinceId: 1,
        regencyId: 1,
        districtId: 1,
        villageId: validPayload.villageId,
        address: validPayload.address
      });

      component.submitRegistration();

      expect(component.formError()).toBe('Identity already exists');
      expect(component.isSuccessOpen()).toBeFalse();
    });
  });

  describe('removeUser', () => {
    it('[positive] removes the row and decrements the total on success', () => {
      fixture.detectChanges();
      userServices.deleteRegistration.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: null }));

      component.removeUser(sampleUser.identity);

      expect(component.users()).toEqual([]);
      expect(component.totalElements()).toBe(0);
      expect(component.deletingIdentity()).toBeNull();
    });

    it('[negative] keeps the row and surfaces an error when deletion fails', () => {
      fixture.detectChanges();
      userServices.deleteRegistration.and.returnValue(throwError(() => new Error('boom')));

      component.removeUser(sampleUser.identity);

      expect(component.users()).toEqual([sampleUser]);
      expect(component.formError()).toBe('Failed to delete employee.');
    });

    it('[negative] does nothing when identity is null', () => {
      fixture.detectChanges();

      component.removeUser(null);

      expect(userServices.deleteRegistration).not.toHaveBeenCalled();
    });
  });

  describe('pagination', () => {
    it('[negative] previousPage is a no-op on the first page', () => {
      fixture.detectChanges();
      userServices.getData.calls.reset();

      component.previousPage();

      expect(userServices.getData).not.toHaveBeenCalled();
    });

    it('[negative] nextPage is a no-op on the last page', () => {
      fixture.detectChanges();
      userServices.getData.calls.reset();

      component.nextPage();

      expect(userServices.getData).not.toHaveBeenCalled();
    });

    it('[positive] goToPage(1) reloads when a second page exists', () => {
      userServices.getData.and.returnValue(of({
        success: true, statusCode: 200, message: 'OK',
        data: { content: [sampleUser], totalElements: 20, totalPages: 2, currentPage: 0, pageSize: 10 }
      }));
      fixture.detectChanges();
      userServices.getData.calls.reset();

      component.goToPage(1);

      expect(component.pageIndex()).toBe(1);
      expect(userServices.getData).toHaveBeenCalledWith(1, 10, '');
    });
  });
});
