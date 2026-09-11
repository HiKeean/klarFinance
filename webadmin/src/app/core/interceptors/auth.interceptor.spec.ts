import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { provideRouter } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { AuthStateService } from '../services/auth-state.service';

/** authState.getAccessToken()/isSessionValid() are real async calls the interceptor awaits
 *  before deciding whether to forward the request - flush microtasks before asserting. */
function flush(): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, 0));
}

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authState: jasmine.SpyObj<AuthStateService>;
  let router: Router;

  beforeEach(() => {
    authState = jasmine.createSpyObj<AuthStateService>('AuthStateService', [
      'getAccessToken',
      'isSessionValid',
      'notifySessionExpired'
    ]);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthStateService, useValue: authState }
      ]
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);
  });

  afterEach(() => httpMock.verify());

  it('[positive] bypasses auth entirely for /auth/login and never checks the session', async () => {
    http.post('/api/v1/internal/auth/login', { identity: 'x', password: 'y' }).subscribe();

    await flush();
    const req = httpMock.expectOne('/api/v1/internal/auth/login');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({ success: true });

    expect(authState.getAccessToken).not.toHaveBeenCalled();
  });

  it('[positive] attaches a Bearer token when the session is valid', async () => {
    authState.getAccessToken.and.resolveTo('valid-token');
    authState.isSessionValid.and.resolveTo(true);

    let received: unknown;
    http.get('/api/v1/admin/auth/users').subscribe((res) => (received = res));

    await flush();
    const req = httpMock.expectOne('/api/v1/admin/auth/users');
    expect(req.request.headers.get('Authorization')).toBe('Bearer valid-token');
    req.flush({ success: true, statusCode: 200, message: 'OK', data: [] });

    await flush();
    expect(received).toBeTruthy();
  });

  it('[negative] never reaches the backend and redirects to /reauth when there is no token', async () => {
    authState.getAccessToken.and.resolveTo(null);
    authState.notifySessionExpired.and.resolveTo(undefined);

    let receivedError: Error | undefined;
    http.get('/api/v1/admin/auth/users').subscribe({
      next: () => fail('expected an error, request should have been short-circuited'),
      error: (err) => (receivedError = err)
    });

    await flush();
    expect(receivedError?.message).toBe('SESSION_EXPIRED');
    expect(authState.notifySessionExpired).toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/reauth');
    httpMock.expectNone('/api/v1/admin/auth/users');
  });

  it('[negative] never reaches the backend and redirects to /reauth when the session is expired', async () => {
    authState.getAccessToken.and.resolveTo('stale-token');
    authState.isSessionValid.and.resolveTo(false);
    authState.notifySessionExpired.and.resolveTo(undefined);

    let receivedError: Error | undefined;
    http.get('/api/v1/admin/auth/users').subscribe({
      next: () => fail('expected an error, request should have been short-circuited'),
      error: (err) => (receivedError = err)
    });

    await flush();
    expect(receivedError?.message).toBe('SESSION_EXPIRED');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/reauth');
  });

  it('[negative] redirects to /reauth when the backend itself responds 401 despite a locally-valid token', async () => {
    authState.getAccessToken.and.resolveTo('valid-but-revoked-token');
    authState.isSessionValid.and.resolveTo(true);
    authState.notifySessionExpired.and.resolveTo(undefined);

    let receivedError: { status?: number } | undefined;
    http.get('/api/v1/admin/auth/users').subscribe({
      next: () => fail('expected an error'),
      error: (err) => (receivedError = err)
    });

    await flush();
    const req = httpMock.expectOne('/api/v1/admin/auth/users');
    req.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    await flush();
    expect(receivedError?.status).toBe(401);
    expect(authState.notifySessionExpired).toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/reauth');
  });
});
