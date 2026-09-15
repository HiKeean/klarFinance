import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { AuthApiRepository } from './auth-api.repository';
import { ApiService } from '../../../core/services/api.service';
import { INTERNAL_URL } from '../../../core/config/url';
import { ApiResponse } from '../../../shared/models/api-response';

describe('AuthApiRepository', () => {
  let api: { post: ReturnType<typeof vi.fn> };
  let repository: AuthApiRepository;

  beforeEach(() => {
    api = { post: vi.fn() };
    TestBed.configureTestingModule({ providers: [AuthApiRepository, { provide: ApiService, useValue: api }] });
    repository = TestBed.inject(AuthApiRepository);
  });

  const credentials = { identity: '123', password: 'secret' };

  function ok<T>(data: T): ApiResponse<T> {
    return { success: true, statusCode: 200, message: '', data };
  }

  it('[positive] a full success response maps through untouched', () => {
    api.post.mockReturnValue(
      of(
        ok({
          accessToken: 'a.b.c',
          refreshToken: 'd.e.f',
          userProfile: { identity: '123', name: 'Budi', role: 'CHECKER' },
          menu: [{ name: 'Dashboard', url: '/dashboard', logo: 'x' }]
        })
      )
    );
    let result: any;
    repository.login(credentials).subscribe((r) => (result = r));
    expect(api.post).toHaveBeenCalledWith(INTERNAL_URL.auth.login, credentials);
    expect(result).toEqual({
      accessToken: 'a.b.c',
      refreshToken: 'd.e.f',
      userProfile: { identity: '123', name: 'Budi', role: 'CHECKER' },
      menu: [{ name: 'Dashboard', url: '/dashboard', logo: 'x' }]
    });
  });

  it('[negative] response.success=false throws the backend message', () => {
    api.post.mockReturnValue(of({ success: false, statusCode: 401, message: 'Identity atau password salah.', data: null }));
    let error: Error | undefined;
    repository.login(credentials).subscribe({ error: (e) => (error = e) });
    expect(error?.message).toBe('Identity atau password salah.');
  });

  it('[negative] missing accessToken/refreshToken in a successful envelope throws "Server Error"', () => {
    api.post.mockReturnValue(of(ok({ userProfile: { identity: '123', name: 'Budi' } })));
    let error: Error | undefined;
    repository.login(credentials).subscribe({ error: (e) => (error = e) });
    expect(error?.message).toBe('Server Error');
  });

  it('[positive] missing userProfile/menu in the DTO falls back to defaults', () => {
    api.post.mockReturnValue(of(ok({ accessToken: 'a.b.c', refreshToken: 'd.e.f' })));
    let result: any;
    repository.login(credentials).subscribe((r) => (result = r));
    expect(result.userProfile).toEqual({ identity: '123', name: 'User' });
    expect(result.menu).toEqual([]);
  });
});
