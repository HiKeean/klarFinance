import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { ApprovalApiService } from './approval-api.service';
import { ApiService } from '../../../core/services/api.service';
import { INTERNAL_URL } from '../../../core/config/url';
import { ApiResponse } from '../../../shared/models/api-response';

describe('ApprovalApiService', () => {
  let service: ApprovalApiService;
  let api: { get: ReturnType<typeof vi.fn>; post: ReturnType<typeof vi.fn>; getBlob: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    api = { get: vi.fn(), post: vi.fn(), getBlob: vi.fn() };
    TestBed.configureTestingModule({
      providers: [ApprovalApiService, { provide: ApiService, useValue: api }]
    });
    service = TestBed.inject(ApprovalApiService);
  });

  function ok<T>(data: T): ApiResponse<T> {
    return { success: true, statusCode: 200, message: '', data };
  }
  function fail(message = 'Gagal memproses.'): ApiResponse<null> {
    return { success: false, statusCode: 400, message, data: null };
  }

  it('[positive] getQueue calls the limit-applications endpoint and unwraps the data', () => {
    const items = [{ id: 1 }];
    api.get.mockReturnValue(of(ok(items)));
    let result: unknown;
    service.getQueue().subscribe((r) => (result = r));
    expect(api.get).toHaveBeenCalledWith(INTERNAL_URL.los.limitApplications);
    expect(result).toEqual(items);
  });

  it('[negative] getQueue throws the backend message when success=false', () => {
    api.get.mockReturnValue(of(fail('Antrean gagal dimuat.')));
    let error: Error | undefined;
    service.getQueue().subscribe({ error: (e) => (error = e) });
    expect(error?.message).toBe('Antrean gagal dimuat.');
  });

  it('[positive] getDetail calls the detail endpoint for the given id and unwraps the data', () => {
    const detail = { id: 42 };
    api.get.mockReturnValue(of(ok(detail)));
    let result: unknown;
    service.getDetail(42).subscribe((r) => (result = r));
    expect(api.get).toHaveBeenCalledWith(INTERNAL_URL.los.limitApplicationDetail(42));
    expect(result).toEqual(detail);
  });

  it('[negative] getDetail throws the backend message when success=false', () => {
    api.get.mockReturnValue(of(fail('Detail tidak ditemukan.')));
    let error: Error | undefined;
    service.getDetail(42).subscribe({ error: (e) => (error = e) });
    expect(error?.message).toBe('Detail tidak ditemukan.');
  });

  it('[positive] getPicture requests a raw blob (no envelope) for the given type', () => {
    const blob = new Blob(['x']);
    api.getBlob.mockReturnValue(of(blob));
    let result: Blob | undefined;
    service.getPicture(7, 'ktp').subscribe((r) => (result = r));
    expect(api.getBlob).toHaveBeenCalledWith(INTERNAL_URL.los.limitApplicationPicture(7, 'ktp'));
    expect(result).toBe(blob);
  });

  it('[positive] checkerDecide posts the decision to the checker-decision endpoint', () => {
    api.post.mockReturnValue(of(ok(null)));
    const request = { action: 'APPROVE' as const, purposeLimit: 1000 };
    let completed = false;
    service.checkerDecide(5, request).subscribe({ complete: () => (completed = true) });
    expect(api.post).toHaveBeenCalledWith(INTERNAL_URL.los.checkerDecision(5), request);
    expect(completed).toBe(true);
  });

  it('[negative] checkerDecide throws the backend message when success=false', () => {
    api.post.mockReturnValue(of(fail('Keputusan checker gagal disimpan.')));
    let error: Error | undefined;
    service.checkerDecide(5, { action: 'REJECT' }).subscribe({ error: (e) => (error = e) });
    expect(error?.message).toBe('Keputusan checker gagal disimpan.');
  });

  it('[positive] bmDecide posts the decision to the bm-decision endpoint', () => {
    api.post.mockReturnValue(of(ok(null)));
    const request = { action: 'APPROVE' as const, finalLimit: 2000 };
    service.bmDecide(9, request).subscribe();
    expect(api.post).toHaveBeenCalledWith(INTERNAL_URL.los.bmDecision(9), request);
  });

  it('[negative] bmDecide throws the backend message when success=false', () => {
    api.post.mockReturnValue(of(fail('Keputusan BM gagal disimpan.')));
    let error: Error | undefined;
    service.bmDecide(9, { action: 'REJECT', reason: 'x' }).subscribe({ error: (e) => (error = e) });
    expect(error?.message).toBe('Keputusan BM gagal disimpan.');
  });

  it('[positive] checkerBreak posts an empty body to the break endpoint', () => {
    api.post.mockReturnValue(of(ok(null)));
    service.checkerBreak().subscribe();
    expect(api.post).toHaveBeenCalledWith(INTERNAL_URL.los.checkerBreak, {});
  });

  it('[negative] checkerBreak throws the backend message when success=false', () => {
    api.post.mockReturnValue(of(fail('Gagal mengambil istirahat.')));
    let error: Error | undefined;
    service.checkerBreak().subscribe({ error: (e) => (error = e) });
    expect(error?.message).toBe('Gagal mengambil istirahat.');
  });

  it('[positive] getLoanReviewDetail calls the loan-review endpoint and unwraps the data', () => {
    const detail = { id: 3 };
    api.get.mockReturnValue(of(ok(detail)));
    let result: unknown;
    service.getLoanReviewDetail(3).subscribe((r) => (result = r));
    expect(api.get).toHaveBeenCalledWith(INTERNAL_URL.fin.loanReviewDetail(3));
    expect(result).toEqual(detail);
  });

  it('[negative] getLoanReviewDetail throws the backend message when success=false', () => {
    api.get.mockReturnValue(of(fail('Review tidak ditemukan.')));
    let error: Error | undefined;
    service.getLoanReviewDetail(3).subscribe({ error: (e) => (error = e) });
    expect(error?.message).toBe('Review tidak ditemukan.');
  });

  it('[positive] loanReviewBmDecide posts the decision to the loan-review bm-decision endpoint', () => {
    api.post.mockReturnValue(of(ok(null)));
    const request = { action: 'APPROVE' as const };
    service.loanReviewBmDecide(11, request).subscribe();
    expect(api.post).toHaveBeenCalledWith(INTERNAL_URL.fin.loanReviewBmDecision(11), request);
  });

  it('[negative] loanReviewBmDecide throws the backend message when success=false', () => {
    api.post.mockReturnValue(of(fail('Gagal menyimpan keputusan review.')));
    let error: Error | undefined;
    service.loanReviewBmDecide(11, { action: 'REJECT', reason: 'x' }).subscribe({ error: (e) => (error = e) });
    expect(error?.message).toBe('Gagal menyimpan keputusan review.');
  });
});
