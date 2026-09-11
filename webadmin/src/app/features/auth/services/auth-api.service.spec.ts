import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthApiService } from './auth-api.service';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { INTERNAL_URL } from '../../../core/config/url';

function flush(): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, 0));
}

describe('AuthApiService', () => {
  let service: AuthApiService;
  let httpMock: HttpTestingController;
  let authState: jasmine.SpyObj<AuthStateService>;

  beforeEach(() => {
    authState = jasmine.createSpyObj<AuthStateService>('AuthStateService', ['saveSession']);
    authState.saveSession.and.resolveTo(undefined);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthStateService, useValue: authState }
      ]
    });
    service = TestBed.inject(AuthApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('[positive] on a successful login with tokens, saves the session with the returned profile/menu', async () => {
    let received: any;
    service.login({ identity: 'superadmin', password: 'secret' }).subscribe((res) => (received = res));

    await flush();
    const req = httpMock.expectOne(INTERNAL_URL.auth.login);
    expect(req.request.body).toEqual({ identity: 'superadmin', password: 'secret' });
    req.flush({
      success: true,
      statusCode: 200,
      message: 'OK',
      data: {
        accessToken: 'a.b.c',
        refreshToken: 'r.e.f',
        userProfile: { identity: 'superadmin', name: 'Super Admin' },
        menu: [{ name: 'User', url: '/dashboard/user', logo: 'person' }]
      }
    });

    await flush();
    expect(authState.saveSession).toHaveBeenCalledWith(
      'a.b.c',
      'r.e.f',
      { identity: 'superadmin', name: 'Super Admin' },
      [{ name: 'User', url: '/dashboard/user', logo: 'person' }]
    );
    expect(received.success).toBeTrue();
  });

  it('[positive] falls back to a minimal profile and empty menu when the backend omits them', async () => {
    service.login({ identity: 'superadmin', password: 'secret' }).subscribe();

    await flush();
    const req = httpMock.expectOne(INTERNAL_URL.auth.login);
    req.flush({
      success: true,
      statusCode: 200,
      message: 'OK',
      data: { accessToken: 'a.b.c', refreshToken: 'r.e.f' }
    });

    await flush();
    expect(authState.saveSession).toHaveBeenCalledWith(
      'a.b.c',
      'r.e.f',
      { identity: 'superadmin', name: 'User' },
      []
    );
  });

  it('[negative] does not save a session when the backend responds success:false (wrong password)', async () => {
    let received: any;
    service.login({ identity: 'superadmin', password: 'wrong' }).subscribe((res) => (received = res));

    await flush();
    const req = httpMock.expectOne(INTERNAL_URL.auth.login);
    req.flush({ success: false, statusCode: 401, message: 'Identity atau password salah.', data: null });

    await flush();
    expect(authState.saveSession).not.toHaveBeenCalled();
    expect(received.success).toBeFalse();
    expect(received.message).toBe('Identity atau password salah.');
  });

  it('[negative] throws "Server Error" when the backend reports success but omits both tokens', async () => {
    let receivedError: Error | undefined;
    service.login({ identity: 'superadmin', password: 'secret' }).subscribe({
      next: () => fail('expected an error'),
      error: (err) => (receivedError = err)
    });

    await flush();
    const req = httpMock.expectOne(INTERNAL_URL.auth.login);
    req.flush({ success: true, statusCode: 200, message: 'OK', data: { userProfile: { identity: 'superadmin', name: 'x' } } });

    await flush();
    expect(receivedError?.message).toBe('Server Error');
    expect(authState.saveSession).not.toHaveBeenCalled();
  });
});
