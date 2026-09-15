import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { PasswordResetApiService } from './password-reset-api.service';
import { ApiService } from '../../../core/services/api.service';
import { AUTH_URL } from '../../../core/config/url';

describe('PasswordResetApiService', () => {
  let api: { post: ReturnType<typeof vi.fn> };
  let service: PasswordResetApiService;

  beforeEach(() => {
    api = { post: vi.fn() };
    TestBed.configureTestingModule({ providers: [PasswordResetApiService, { provide: ApiService, useValue: api }] });
    service = TestBed.inject(PasswordResetApiService);
  });

  it('[positive] submit posts the identity to the password-reset-request endpoint', () => {
    api.post.mockReturnValue(of({ success: true, statusCode: 200, message: '', data: null }));
    let completed = false;
    service.submit('123').subscribe({ complete: () => (completed = true) });
    expect(api.post).toHaveBeenCalledWith(AUTH_URL.passwordResetRequest, { identity: '123' });
    expect(completed).toBe(true);
  });

  it('[negative] response.success=false throws the backend message', () => {
    api.post.mockReturnValue(of({ success: false, statusCode: 400, message: 'Identity tidak ditemukan.', data: null }));
    let error: Error | undefined;
    service.submit('123').subscribe({ error: (e) => (error = e) });
    expect(error?.message).toBe('Identity tidak ditemukan.');
  });
});
