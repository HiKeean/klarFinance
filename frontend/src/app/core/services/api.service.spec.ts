import { HttpParams, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApiService } from './api.service';
import { ApiResponse } from '../../shared/models/api-response';

/** createHeaders() awaits crypto.subtle (native async work, not just a microtask) before the
 *  request is actually sent - give it a real event-loop turn before asserting on httpMock. */
function flush(): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, 100));
}

describe('ApiService', () => {
  let service: ApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ApiService, provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  const envelope: ApiResponse<{ ok: boolean }> = { success: true, statusCode: 200, message: '', data: { ok: true } };

  it('[positive] GET attaches HMAC-derived headers (timestamp, signature, client-type)', async () => {
    let result: ApiResponse<{ ok: boolean }> | undefined;
    service.get<{ ok: boolean }>('/foo').subscribe((res) => (result = res));

    await flush();
    const req = httpMock.expectOne('/foo');
    expect(req.request.headers.get('X-Client-Type')).toBe('ANGULAR');
    expect(req.request.headers.has('X-Timestamp')).toBe(true);
    expect(req.request.headers.has('X-Signature')).toBe(true);
    req.flush(envelope);

    expect(result).toEqual(envelope);
  });

  it('[positive] GET with a plain object appends it as a query string', async () => {
    // withParams() bakes the query string into the URL text itself (not the HttpRequest's
    // `params` option), so assert against urlWithParams rather than r.params.
    service.get('/search', { q: 'budi', page: 1 }).subscribe();
    await flush();
    const req = httpMock.expectOne((r) => r.urlWithParams === '/search?q=budi&page=1');
    req.flush(envelope);
  });

  it('[positive] GET with an existing "?" in the url appends params with "&"', async () => {
    service.get('/search?sort=asc', { q: 'x' }).subscribe();
    await flush();
    const req = httpMock.expectOne((r) => r.urlWithParams === '/search?sort=asc&q=x');
    req.flush(envelope);
  });

  it('[positive] GET with an HttpParams instance is serialized the same way', async () => {
    const params = new HttpParams().set('q', 'x');
    service.get('/search2', params).subscribe();
    await flush();
    const req = httpMock.expectOne((r) => r.urlWithParams === '/search2?q=x');
    req.flush(envelope);
  });

  it('[positive] POST sends the body and headers', async () => {
    let result: ApiResponse<{ ok: boolean }> | undefined;
    service.post<{ ok: boolean }>('/create', { name: 'a' }).subscribe((res) => (result = res));

    await flush();
    const req = httpMock.expectOne('/create');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ name: 'a' });
    expect(req.request.headers.has('X-Signature')).toBe(true);
    req.flush(envelope);

    expect(result).toEqual(envelope);
  });

  it('[positive] DELETE issues a DELETE request', async () => {
    service.delete('/item/1').subscribe();
    await flush();
    const req = httpMock.expectOne('/item/1');
    expect(req.request.method).toBe('DELETE');
    req.flush(envelope);
  });

  it('[positive] PUT issues a PUT request with a body', async () => {
    service.put('/item/1', { name: 'b' }).subscribe();
    await flush();
    const req = httpMock.expectOne('/item/1');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ name: 'b' });
    req.flush(envelope);
  });

  it('[positive] getBlob requests a blob response type', async () => {
    service.getBlob('/picture/1').subscribe();
    await flush();
    const req = httpMock.expectOne('/picture/1');
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['x']));
  });
});
