import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ApiService } from './api.service';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../../shared/models/api-response';

/** ApiService signs headers via crypto.subtle (a real async Web Crypto call), so the actual
 *  HttpClient call fires a tick after subscribe() rather than synchronously - flush microtasks
 *  before asserting against HttpTestingController. */
function flush(): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, 0));
}

describe('ApiService', () => {
  let service: ApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('positive: signed headers', () => {
    it('attaches X-Timestamp, X-Signature and X-Client-Type headers on GET', async () => {
      let received: ApiResponse<{ ok: boolean }> | undefined;
      service.get<{ ok: boolean }>('https://api.test/foo').subscribe((res) => (received = res));

      await flush();
      const req = httpMock.expectOne('https://api.test/foo');
      expect(req.request.method).toBe('GET');
      expect(req.request.headers.get('X-Client-Type')).toBe('ANGULAR');
      expect(req.request.headers.get('X-Timestamp')).toMatch(/^\d+$/);
      expect(req.request.headers.get('X-Signature')).toBeTruthy();
      req.flush({ success: true, statusCode: 200, message: 'OK', data: { ok: true } });

      await flush();
      expect(received?.data.ok).toBeTrue();
    });

    it('appends query params for GET as a Record<string, primitive>', async () => {
      service.get<unknown>('https://api.test/foo', { page: 0, size: 10, search: 'abc' }).subscribe();

      await flush();
      const req = httpMock.expectOne('https://api.test/foo?page=0&size=10&search=abc');
      expect(req.request.method).toBe('GET');
      req.flush({ success: true, statusCode: 200, message: 'OK', data: null });
    });

    it('sends a JSON body on POST and signs it', async () => {
      const payload = { name: 'Approval' };
      service.post<unknown>('https://api.test/menu', payload).subscribe();

      await flush();
      const req = httpMock.expectOne('https://api.test/menu');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(payload);
      expect(req.request.headers.get('X-Signature')).toBeTruthy();
      req.flush({ success: true, statusCode: 200, message: 'OK', data: null });
    });

    it('issues a DELETE request with query params instead of a body', async () => {
      service.delete<unknown>('https://api.test/role/1', { identity: 'abc' }).subscribe();

      await flush();
      const req = httpMock.expectOne('https://api.test/role/1?identity=abc');
      expect(req.request.method).toBe('DELETE');
      req.flush({ success: true, statusCode: 200, message: 'OK', data: null });
    });

    it('issues a PUT request with the given body', async () => {
      service.put<unknown>('https://api.test/role/1', { role: 'BM' }).subscribe();

      await flush();
      const req = httpMock.expectOne('https://api.test/role/1');
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ role: 'BM' });
      req.flush({ success: true, statusCode: 200, message: 'OK', data: null });
    });

    it('signs with the configured environment apiKey/secretKey (sanity check on environment wiring)', () => {
      expect(environment.api.apiKey).toBeDefined();
      expect(environment.api.secretKey).toBeDefined();
    });
  });

  describe('negative: transport failures', () => {
    it('propagates an HTTP error response to the subscriber', async () => {
      let receivedError: { status?: number } | undefined;
      service.get<unknown>('https://api.test/fail').subscribe({
        next: () => fail('expected an error, got a next value'),
        error: (err) => (receivedError = err)
      });

      await flush();
      const req = httpMock.expectOne('https://api.test/fail');
      req.flush('Internal Server Error', { status: 500, statusText: 'Server Error' });

      await flush();
      expect(receivedError?.status).toBe(500);
    });

    it('does not silently swallow a 401 - it still reaches the caller as an error', async () => {
      let receivedError: { status?: number } | undefined;
      service.get<unknown>('https://api.test/secure').subscribe({
        next: () => fail('expected an error, got a next value'),
        error: (err) => (receivedError = err)
      });

      await flush();
      const req = httpMock.expectOne('https://api.test/secure');
      req.flush({ message: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

      await flush();
      expect(receivedError?.status).toBe(401);
    });
  });
});
