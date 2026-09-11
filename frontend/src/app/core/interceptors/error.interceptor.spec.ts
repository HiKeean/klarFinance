import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { errorInterceptor } from './error.interceptor';

describe('errorInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting()
      ]
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('[positive] passes through a successful response untouched', async () => {
    const result$ = firstValueFrom(http.get('/api/ok'));
    httpMock.expectOne('/api/ok').flush({ hello: 'world' });
    await expect(result$).resolves.toEqual({ hello: 'world' });
  });

  it('[positive] surfaces a short backend message as-is', async () => {
    const result$ = firstValueFrom(http.get('/api/fail'));
    httpMock
      .expectOne('/api/fail')
      .flush({ message: 'Identity atau password salah.' }, { status: 400, statusText: 'Bad Request' });
    await expect(result$).rejects.toMatchObject({ message: 'Identity atau password salah.' });
  });

  it('[negative] bypasses the length cap in dev mode and surfaces the raw backend message', async () => {
    // isDevMode() is true under the test builder (prod mode is never enabled here), so a
    // message over MAX_USER_MESSAGE_LENGTH is still returned as-is instead of the generic fallback.
    const longMessage = 'x'.repeat(250);
    const result$ = firstValueFrom(http.get('/api/fail-long'));
    httpMock
      .expectOne('/api/fail-long')
      .flush({ message: longMessage }, { status: 500, statusText: 'Server Error' });
    await expect(result$).rejects.toMatchObject({ message: longMessage });
  });

  it('[negative] falls back to the HttpErrorResponse message when the backend body has no message field', async () => {
    const result$ = firstValueFrom(http.get('/api/fail-empty'));
    httpMock.expectOne('/api/fail-empty').flush('some html error page', { status: 502, statusText: 'Bad Gateway' });
    await expect(result$).rejects.toThrow(/502/);
  });
});
