import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { errorInterceptor } from './error.interceptor';

/** Kept separate from error.interceptor.spec.ts (which is not to be edited) - covers the
 *  extractBackendMessage branches the original spec doesn't reach: a non-plain-object error body
 *  (ProgressEvent, e.g. a network-level failure) and a body whose `message` isn't a string. */
describe('errorInterceptor edge cases', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([errorInterceptor])), provideHttpClientTesting()]
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('[negative] a ProgressEvent error body (network-level failure) falls back to the HttpErrorResponse message', async () => {
    const result$ = firstValueFrom(http.get('/api/network-fail'));
    const req = httpMock.expectOne('/api/network-fail');
    req.error(new ProgressEvent('error'), { status: 0, statusText: 'Unknown Error' });
    await expect(result$).rejects.toBeTruthy();
  });

  it('[negative] a body whose message field is not a string is ignored (falls back)', async () => {
    const result$ = firstValueFrom(http.get('/api/weird-body'));
    const req = httpMock.expectOne('/api/weird-body');
    req.flush({ message: 12345 }, { status: 500, statusText: 'Server Error' });
    await expect(result$).rejects.toBeTruthy();
  });
});
