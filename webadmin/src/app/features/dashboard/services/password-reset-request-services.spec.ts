import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { PasswordResetRequestServices } from './password-reset-request-services';
import { ApiService } from '../../../core/services/api.service';
import { SUPERADMIN_URL } from '../../../core/config/url';

describe('PasswordResetRequestServices', () => {
  let service: PasswordResetRequestServices;
  let api: jasmine.SpyObj<ApiService>;

  beforeEach(() => {
    api = jasmine.createSpyObj<ApiService>('ApiService', ['get', 'post', 'put', 'delete']);
    TestBed.configureTestingModule({ providers: [{ provide: ApiService, useValue: api }] });
    service = TestBed.inject(PasswordResetRequestServices);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getAll', () => {
    it('[positive] defaults the status filter to PENDING', () => {
      api.get.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [] }));

      service.getAll().subscribe();

      expect(api.get).toHaveBeenCalledWith(SUPERADMIN_URL.auth.passwordResetRequests, { status: 'PENDING' });
    });

    it('[positive] passes through an explicit status filter', () => {
      api.get.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [] }));

      service.getAll('APPROVED').subscribe();

      expect(api.get).toHaveBeenCalledWith(SUPERADMIN_URL.auth.passwordResetRequests, { status: 'APPROVED' });
    });

    it('[negative] propagates a transport error', (done) => {
      api.get.and.returnValue(throwError(() => new Error('boom')));

      service.getAll().subscribe({
        next: () => fail('expected an error'),
        error: (err: Error) => {
          expect(err.message).toBe('boom');
          done();
        }
      });
    });
  });

  describe('decide', () => {
    it('[positive] posts APPROVE without a reason', () => {
      api.post.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: null }));

      service.decide(5, 'APPROVE').subscribe();

      expect(api.post).toHaveBeenCalledWith(SUPERADMIN_URL.auth.passwordResetRequestDecision(5), { action: 'APPROVE', reason: undefined });
    });

    it('[positive] posts REJECT with a reason', () => {
      api.post.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: null }));

      service.decide(5, 'REJECT', 'Tidak sesuai prosedur').subscribe();

      expect(api.post).toHaveBeenCalledWith(SUPERADMIN_URL.auth.passwordResetRequestDecision(5), {
        action: 'REJECT',
        reason: 'Tidak sesuai prosedur'
      });
    });

    it('[negative] passes through a success:false decision response', (done) => {
      api.post.and.returnValue(of({ success: false, statusCode: 409, message: 'Sudah diputuskan sebelumnya', data: null }));

      service.decide(5, 'APPROVE').subscribe((res) => {
        expect(res.success).toBeFalse();
        expect(res.message).toBe('Sudah diputuskan sebelumnya');
        done();
      });
    });
  });
});
