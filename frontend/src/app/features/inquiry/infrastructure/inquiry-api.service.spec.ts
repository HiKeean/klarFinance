import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { InquiryApiService } from './inquiry-api.service';
import { ApiService } from '../../../core/services/api.service';
import { INTERNAL_URL } from '../../../core/config/url';

describe('InquiryApiService', () => {
  let api: { get: ReturnType<typeof vi.fn> };
  let service: InquiryApiService;

  beforeEach(() => {
    api = { get: vi.fn() };
    TestBed.configureTestingModule({ providers: [InquiryApiService, { provide: ApiService, useValue: api }] });
    service = TestBed.inject(InquiryApiService);
  });

  it('[positive] search calls the limit-applications search endpoint with q and unwraps the data', () => {
    const results = [{ id: 1 }];
    api.get.mockReturnValue(of({ success: true, statusCode: 200, message: '', data: results }));
    let result: unknown;
    service.search('budi').subscribe((r) => (result = r));
    expect(api.get).toHaveBeenCalledWith(INTERNAL_URL.los.limitApplicationsSearch, { q: 'budi' });
    expect(result).toEqual(results);
  });

  it('[negative] search throws the backend message when success=false', () => {
    api.get.mockReturnValue(of({ success: false, statusCode: 400, message: 'Gagal mencari.', data: null }));
    let error: Error | undefined;
    service.search('budi').subscribe({ error: (e) => (error = e) });
    expect(error?.message).toBe('Gagal mencari.');
  });
});
