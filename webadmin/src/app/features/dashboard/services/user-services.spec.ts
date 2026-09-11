import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { UserServices } from './user-services';
import { ApiService } from '../../../core/services/api.service';
import { SUPERADMIN_URL } from '../../../core/config/url';

describe('UserServices', () => {
  let service: UserServices;
  let api: jasmine.SpyObj<ApiService>;

  beforeEach(() => {
    api = jasmine.createSpyObj<ApiService>('ApiService', ['get', 'post', 'put', 'delete']);
    TestBed.configureTestingModule({ providers: [{ provide: ApiService, useValue: api }] });
    service = TestBed.inject(UserServices);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('positive', () => {
    it('getData requests the given page/size without a search param when search is empty', () => {
      api.get.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: null as any }));

      service.getData(2, 10).subscribe();

      expect(api.get).toHaveBeenCalledWith(SUPERADMIN_URL.auth.getAllUsers, { page: 2, size: 10 });
    });

    it('getData includes the search term when provided', () => {
      api.get.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: null as any }));

      service.getData(0, 10, 'budi').subscribe();

      expect(api.get).toHaveBeenCalledWith(SUPERADMIN_URL.auth.getAllUsers, { page: 0, size: 10, search: 'budi' });
    });

    it('registerEmployee posts the payload to the registration endpoint', () => {
      api.post.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: null }));
      const payload = { name: 'Budi', role: 'BM', noHp: '0812', dob: '2000-01-01', password: 'secret1', branchId: 1, villageId: 2, address: 'Jl. A' };

      service.registerEmployee(payload).subscribe();

      expect(api.post).toHaveBeenCalledWith(SUPERADMIN_URL.auth.registration, payload);
    });

    it('deleteRegistration sends the identity as a query param', () => {
      api.delete.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: null }));

      service.deleteRegistration('2026000001').subscribe();

      expect(api.delete).toHaveBeenCalledWith(SUPERADMIN_URL.auth.deleteRegistration, { identity: '2026000001' });
    });
  });

  describe('negative', () => {
    it('getData propagates a transport error from ApiService', (done) => {
      api.get.and.returnValue(throwError(() => new Error('network down')));

      service.getData(0, 10).subscribe({
        next: () => fail('expected an error'),
        error: (err) => {
          expect(err.message).toBe('network down');
          done();
        }
      });
    });

    it('registerEmployee passes through a success:false response instead of throwing', (done) => {
      api.post.and.returnValue(of({ success: false, statusCode: 409, message: 'Identity already exists', data: null }));

      service.registerEmployee({ name: 'Budi', role: 'BM', noHp: '0812', dob: '2000-01-01', password: 'secret1', branchId: 1, villageId: 2, address: 'Jl. A' })
        .subscribe((res) => {
          expect(res.success).toBeFalse();
          expect(res.message).toBe('Identity already exists');
          done();
        });
    });
  });
});
