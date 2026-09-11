import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { RoleServices } from './role-services';
import { ApiService } from '../../../core/services/api.service';
import { SUPERADMIN_URL } from '../../../core/config/url';

describe('RoleServices', () => {
  let service: RoleServices;
  let api: jasmine.SpyObj<ApiService>;

  beforeEach(() => {
    api = jasmine.createSpyObj<ApiService>('ApiService', ['get', 'post', 'put', 'delete']);
    TestBed.configureTestingModule({ providers: [{ provide: ApiService, useValue: api }] });
    service = TestBed.inject(RoleServices);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('positive', () => {
    it('getAllRoles calls the roles endpoint', () => {
      api.get.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [] }));

      service.getAllRoles().subscribe();

      expect(api.get).toHaveBeenCalledWith(SUPERADMIN_URL.auth.getAllRoles);
    });

    it('saveRole posts the role name', () => {
      api.post.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: null }));

      service.saveRole('FINANCE').subscribe();

      expect(api.post).toHaveBeenCalledWith(SUPERADMIN_URL.auth.saveRole, { role: 'FINANCE' });
    });

    it('updateRole PUTs to the role-specific URL with the new name', () => {
      api.put.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: null }));

      service.updateRole(5, 'BM_SENIOR').subscribe();

      expect(api.put).toHaveBeenCalledWith(SUPERADMIN_URL.auth.updateRole(5), { role: 'BM_SENIOR' });
    });

    it('deleteRole calls the role-specific delete URL', () => {
      api.delete.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: null }));

      service.deleteRole(5).subscribe();

      expect(api.delete).toHaveBeenCalledWith(SUPERADMIN_URL.auth.deleteRole(5));
    });
  });

  describe('negative', () => {
    it('deleteRole propagates a transport error', (done) => {
      api.delete.and.returnValue(throwError(() => new Error('boom')));

      service.deleteRole(5).subscribe({
        next: () => fail('expected an error'),
        error: (err) => {
          expect(err.message).toBe('boom');
          done();
        }
      });
    });

    it('saveRole passes through a success:false (duplicate role) response', (done) => {
      api.post.and.returnValue(of({ success: false, statusCode: 409, message: 'Role already exists', data: null }));

      service.saveRole('BM').subscribe((res) => {
        expect(res.success).toBeFalse();
        expect(res.message).toBe('Role already exists');
        done();
      });
    });
  });
});
