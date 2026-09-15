import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { authInterceptor } from './auth.interceptor';
import { AuthStateService } from '../services/auth-state.service';

/** The interceptor's token/session checks are async (Promises resolved inside a `switchMap`),
 *  so the actual HTTP request isn't dispatched to the backend synchronously - give it a real
 *  event-loop turn before asserting on httpMock, same convention as api.service.spec.ts. */
function flush(): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, 50));
}

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authState: { getAccessToken: ReturnType<typeof vi.fn>; isSessionValid: ReturnType<typeof vi.fn>; notifySessionExpired: ReturnType<typeof vi.fn> };
  let router: Router;

  beforeEach(() => {
    authState = { getAccessToken: vi.fn(), isSessionValid: vi.fn(), notifySessionExpired: vi.fn().mockResolvedValue(undefined) };
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
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
  });

  afterEach(() => httpMock.verify());

  it('[positive] bypasses auth entirely for the login endpoint', async () => {
    const result$ = firstValueFrom(http.post('/auth/login', {}));
    const req = httpMock.expectOne('/auth/login');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({ ok: true });
    await result$;
    expect(authState.getAccessToken).not.toHaveBeenCalled();
  });

  it('[positive] attaches a Bearer token when the session is valid', async () => {
    authState.getAccessToken.mockResolvedValue('abc123');
    authState.isSessionValid.mockResolvedValue(true);
    const result$ = firstValueFrom(http.get('/api/data'));
    await flush();
    const req = httpMock.expectOne('/api/data');
    expect(req.request.headers.get('Authorization')).toBe('Bearer abc123');
    req.flush({ ok: true });
    await result$;
  });

  it('[negative] missing token notifies expiry, navigates to /reauth, and errors without sending a request', async () => {
    authState.getAccessToken.mockResolvedValue(null);
    const result$ = firstValueFrom(http.get('/api/data'));
    await expect(result$).rejects.toThrow('SESSION_EXPIRED');
    await flush();
    expect(authState.notifySessionExpired).toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/reauth');
    httpMock.expectNone('/api/data');
  });

  it('[negative] an invalid session notifies expiry, navigates to /reauth, and errors without sending a request', async () => {
    authState.getAccessToken.mockResolvedValue('abc123');
    authState.isSessionValid.mockResolvedValue(false);
    const result$ = firstValueFrom(http.get('/api/data'));
    await expect(result$).rejects.toThrow('SESSION_EXPIRED');
    await flush();
    expect(authState.notifySessionExpired).toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/reauth');
    httpMock.expectNone('/api/data');
  });

  it('[negative] a downstream 401 response notifies expiry and navigates to /reauth', async () => {
    authState.getAccessToken.mockResolvedValue('abc123');
    authState.isSessionValid.mockResolvedValue(true);
    const result$ = firstValueFrom(http.get('/api/data'));
    await flush();
    const req = httpMock.expectOne('/api/data');
    req.flush({ message: 'unauthorized' }, { status: 401, statusText: 'Unauthorized' });
    await expect(result$).rejects.toBeTruthy();
    expect(authState.notifySessionExpired).toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/reauth');
  });

  it('[negative] a downstream 403 response notifies expiry and navigates to /reauth', async () => {
    authState.getAccessToken.mockResolvedValue('abc123');
    authState.isSessionValid.mockResolvedValue(true);
    const result$ = firstValueFrom(http.get('/api/data'));
    await flush();
    const req = httpMock.expectOne('/api/data');
    req.flush({ message: 'forbidden' }, { status: 403, statusText: 'Forbidden' });
    await expect(result$).rejects.toBeTruthy();
    expect(authState.notifySessionExpired).toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/reauth');
  });

  it('[positive] other error codes are rethrown untouched without notifying expiry', async () => {
    authState.getAccessToken.mockResolvedValue('abc123');
    authState.isSessionValid.mockResolvedValue(true);
    const result$ = firstValueFrom(http.get('/api/data'));
    await flush();
    const req = httpMock.expectOne('/api/data');
    req.flush({ message: 'server error' }, { status: 500, statusText: 'Server Error' });
    await expect(result$).rejects.toBeTruthy();
    expect(authState.notifySessionExpired).not.toHaveBeenCalled();
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });
});
