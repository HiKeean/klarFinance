import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { DashboardApiService } from './dashboard-api.service';
import { ApiService } from '../../../core/services/api.service';
import { INTERNAL_URL } from '../../../core/config/url';

describe('DashboardApiService', () => {
  let api: { get: ReturnType<typeof vi.fn> };
  let service: DashboardApiService;

  beforeEach(() => {
    api = { get: vi.fn() };
    TestBed.configureTestingModule({ providers: [DashboardApiService, { provide: ApiService, useValue: api }] });
    service = TestBed.inject(DashboardApiService);
  });

  it('[positive] getSummary calls the dashboard-summary endpoint and unwraps the data', () => {
    const summary = { needsToReview: 3 };
    api.get.mockReturnValue(of({ success: true, statusCode: 200, message: '', data: summary }));
    let result: unknown;
    service.getSummary().subscribe((r) => (result = r));
    expect(api.get).toHaveBeenCalledWith(INTERNAL_URL.los.dashboardSummary);
    expect(result).toEqual(summary);
  });

  it('[negative] getSummary throws the backend message when success=false', () => {
    api.get.mockReturnValue(of({ success: false, statusCode: 400, message: 'Gagal memuat ringkasan.', data: null }));
    let error: Error | undefined;
    service.getSummary().subscribe({ error: (e) => (error = e) });
    expect(error?.message).toBe('Gagal memuat ringkasan.');
  });
});
