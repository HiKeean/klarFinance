import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { InquiryListPage } from './inquiry-list';
import { InquiryApiService } from '../../../infrastructure/inquiry-api.service';
import { InquiryResult } from '../../../domain/entities/inquiry-result';

describe('InquiryListPage', () => {
  let inquiryApi: { search: ReturnType<typeof vi.fn> };
  let router: Router;
  let page: InquiryListPage;

  beforeEach(() => {
    inquiryApi = { search: vi.fn() };
    TestBed.configureTestingModule({
      providers: [InquiryListPage, provideRouter([]), { provide: InquiryApiService, useValue: inquiryApi }]
    });
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    page = TestBed.inject(InquiryListPage);
  });

  it('[negative] an empty query clears results/hasSearched/errorMessage without calling the API', () => {
    (page as any).errorMessage.set('previous error');
    (page as any).onSearch('');
    expect((page as any).results()).toEqual([]);
    expect((page as any).hasSearched()).toBe(false);
    expect((page as any).errorMessage()).toBe('');
    expect(inquiryApi.search).not.toHaveBeenCalled();
  });

  it('[positive] a non-empty query calls search and populates results', () => {
    const results: InquiryResult[] = [{ id: 1, applicationCode: 'A1', customerName: 'Budi', customerIdentity: '123', status: 'PENDING_CHECKER' }];
    inquiryApi.search.mockReturnValue(of(results));
    (page as any).onSearch('budi');
    expect(inquiryApi.search).toHaveBeenCalledWith('budi');
    expect((page as any).results()).toEqual(results);
    expect((page as any).hasSearched()).toBe(true);
    expect((page as any).loading()).toBe(false);
  });

  it('[negative] a search error sets errorMessage and stops loading', () => {
    inquiryApi.search.mockReturnValue(throwError(() => new Error('Gagal mencari khusus.')));
    (page as any).onSearch('budi');
    expect((page as any).errorMessage()).toBe('Gagal mencari khusus.');
    expect((page as any).loading()).toBe(false);
  });

  it('[negative] a search error without a message falls back to a generic message', () => {
    inquiryApi.search.mockReturnValue(throwError(() => ({})));
    (page as any).onSearch('budi');
    expect((page as any).errorMessage()).toBe('Gagal mencari aplikasi.');
  });

  it('[positive] statusLabel maps known statuses', () => {
    expect((page as any).statusLabel('PENDING_CHECKER')).toBe('Menunggu Checker');
    expect((page as any).statusLabel('APPROVED')).toBe('Disetujui');
  });

  it('[negative] statusLabel falls back to the raw status for an unknown key', () => {
    expect((page as any).statusLabel('WEIRD_STATUS')).toBe('WEIRD_STATUS');
  });

  it('[positive] openDetail navigates to /approval/:id', () => {
    (page as any).openDetail({ id: 42, applicationCode: null, customerName: null, customerIdentity: '1', status: 'APPROVED' });
    expect(router.navigate).toHaveBeenCalledWith(['/approval', 42]);
  });
});
