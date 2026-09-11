import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { RoleMenuService } from './role-menu-services';
import { ApiService } from '../../../core/services/api.service';
import { SUPERADMIN_URL } from '../../../core/config/url';

describe('RoleMenuService', () => {
  let service: RoleMenuService;
  let api: jasmine.SpyObj<ApiService>;

  beforeEach(() => {
    api = jasmine.createSpyObj<ApiService>('ApiService', ['get', 'post', 'put', 'delete']);
    TestBed.configureTestingModule({ providers: [{ provide: ApiService, useValue: api }] });
    service = TestBed.inject(RoleMenuService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('positive', () => {
    it('getAllRoleMenus calls the endpoint with no params when no filter is given', () => {
      api.get.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [] }));

      service.getAllRoleMenus().subscribe();

      expect(api.get).toHaveBeenCalledWith(SUPERADMIN_URL.auth.getAllRoleMenus, {});
    });

    it('getAllRoleMenus only includes the filter fields that are actually set', () => {
      api.get.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [] }));

      service.getAllRoleMenus({ role: 'BM' }).subscribe();

      expect(api.get).toHaveBeenCalledWith(SUPERADMIN_URL.auth.getAllRoleMenus, { role: 'BM' });
    });

    it('addRoleMenu posts role+menu name', () => {
      api.post.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: null }));

      service.addRoleMenu('BM', 'Approval (BM)').subscribe();

      expect(api.post).toHaveBeenCalledWith(SUPERADMIN_URL.auth.addRoleMenu, { role: 'BM', menu: 'Approval (BM)' });
    });

    it('deleteRoleMenu calls the role-menu-specific delete URL', () => {
      api.delete.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: null }));

      service.deleteRoleMenu(8).subscribe();

      expect(api.delete).toHaveBeenCalledWith(SUPERADMIN_URL.auth.deleteRoleMenu(8));
    });
  });

  describe('negative', () => {
    it('getAllRoleMenus propagates a transport error', (done) => {
      api.get.and.returnValue(throwError(() => new Error('boom')));

      service.getAllRoleMenus({ role: 'BM' }).subscribe({
        next: () => fail('expected an error'),
        error: (err) => {
          expect(err.message).toBe('boom');
          done();
        }
      });
    });

    it('addRoleMenu passes through a success:false (already assigned) response', (done) => {
      api.post.and.returnValue(of({ success: false, statusCode: 409, message: 'Already assigned', data: null }));

      service.addRoleMenu('BM', 'Approval (BM)').subscribe((res) => {
        expect(res.success).toBeFalse();
        done();
      });
    });
  });
});
