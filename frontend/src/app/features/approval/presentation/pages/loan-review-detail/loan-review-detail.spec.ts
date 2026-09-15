import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { LoanReviewDetailPage } from './loan-review-detail';
import { ApprovalApiService } from '../../../infrastructure/approval-api.service';
import { LoanReviewDetail } from '../../../domain/entities/limit-application';

function detail(overrides: Partial<LoanReviewDetail> = {}): LoanReviewDetail {
  return {
    id: 1,
    status: 'PENDING_BM',
    customerIdentity: '123',
    customerName: 'Budi',
    customerAddress: null,
    provinceName: null,
    regencyName: null,
    requestedAmount: 1_000_000,
    tenorMonths: 6,
    bankAccountNumber: null,
    bankCode: null,
    usedLimitSnapshot: 0,
    totalLimitSnapshot: 5_000_000,
    utilizationPercent: 0,
    projectedTotalDebt: 1_000_000,
    pefindoScore: null,
    pefindoColStatus: null,
    pefindoRiskLabel: null,
    pinjolAppsCount: 0,
    bankingAppsCount: 0,
    pinjolApps: [],
    bankApps: [],
    bmReason: null,
    createdAt: '2026-01-01',
    lockedByIdentity: null,
    lockedByName: null,
    lockedByMe: false,
    canReview: true,
    ...overrides
  };
}

describe('LoanReviewDetailPage', () => {
  let approvalApi: { getLoanReviewDetail: ReturnType<typeof vi.fn>; loanReviewBmDecide: ReturnType<typeof vi.fn> };
  let router: Router;

  function setup(detailResult: Observable<LoanReviewDetail> = of(detail())) {
    approvalApi = {
      getLoanReviewDetail: vi.fn().mockReturnValue(detailResult),
      loanReviewBmDecide: vi.fn().mockReturnValue(of(undefined))
    };
    TestBed.configureTestingModule({
      providers: [
        LoanReviewDetailPage,
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: new Map([['id', '1']]) } } },
        { provide: ApprovalApiService, useValue: approvalApi }
      ]
    });
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    return TestBed.inject(LoanReviewDetailPage);
  }

  it('[positive] loads the review detail and populates the signal', () => {
    const page = setup();
    expect(approvalApi.getLoanReviewDetail).toHaveBeenCalledWith(1);
    expect((page as any).detail()).toEqual(detail());
    expect((page as any).loading()).toBe(false);
  });

  it('[negative] load error sets errorMessage and stops loading', () => {
    const page = setup(throwError(() => new Error('Gagal memuat detail pengajuan khusus.')));
    expect((page as any).errorMessage()).toBe('Gagal memuat detail pengajuan khusus.');
    expect((page as any).loading()).toBe(false);
  });

  it('[negative] load error without a message falls back to a generic message', () => {
    const page = setup(throwError(() => ({})));
    expect((page as any).errorMessage()).toBe('Gagal memuat detail pengajuan.');
  });

  it('[positive] canDecide is true only at PENDING_BM', () => {
    const page = setup(of(detail({ status: 'PENDING_BM' })));
    expect((page as any).canDecide()).toBe(true);
  });

  it('[negative] canDecide is false for other statuses', () => {
    const page = setup(of(detail({ status: 'APPROVED' })));
    expect((page as any).canDecide()).toBe(false);
  });

  it('[positive] openForm sets formOpen and actionType, cancelForm resets them', () => {
    const page = setup();
    (page as any).openForm('REJECT');
    expect((page as any).formOpen()).toBe(true);
    expect((page as any).actionType()).toBe('REJECT');
    (page as any).cancelForm();
    expect((page as any).formOpen()).toBe(false);
    expect((page as any).actionType()).toBeNull();
  });

  it('[negative] cancelForm is a no-op while submitting', () => {
    const page = setup();
    (page as any).openForm('REJECT');
    (page as any).submitting.set(true);
    (page as any).cancelForm();
    expect((page as any).formOpen()).toBe(true);
  });

  it('[negative] confirm REJECT without a reason sets an error and does not call the API', () => {
    const page = setup();
    (page as any).openForm('REJECT');
    (page as any).confirm();
    expect((page as any).actionError()).toBe('Alasan reject wajib diisi.');
    expect(approvalApi.loanReviewBmDecide).not.toHaveBeenCalled();
  });

  it('[positive] confirm REJECT with a reason calls loanReviewBmDecide and navigates back', () => {
    const page = setup();
    (page as any).openForm('REJECT');
    (page as any).reasonInput.set('data tidak valid');
    (page as any).confirm();
    expect(approvalApi.loanReviewBmDecide).toHaveBeenCalledWith(1, { action: 'REJECT', reason: 'data tidak valid' });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/bm/approval');
  });

  it('[positive] confirm APPROVE calls loanReviewBmDecide with an undefined reason', () => {
    const page = setup();
    (page as any).openForm('APPROVE');
    (page as any).confirm();
    expect(approvalApi.loanReviewBmDecide).toHaveBeenCalledWith(1, { action: 'APPROVE', reason: undefined });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/bm/approval');
  });

  it('[negative] confirm API error sets actionError and resets submitting', () => {
    const page = setup();
    approvalApi.loanReviewBmDecide.mockReturnValue(throwError(() => new Error('Gagal menyimpan keputusan review khusus.')));
    (page as any).openForm('APPROVE');
    (page as any).confirm();
    expect((page as any).actionError()).toBe('Gagal menyimpan keputusan review khusus.');
    expect((page as any).submitting()).toBe(false);
  });

  it('[negative] confirm is a no-op without an actionType set', () => {
    const page = setup();
    (page as any).confirm();
    expect(approvalApi.loanReviewBmDecide).not.toHaveBeenCalled();
  });

  it('[positive] goBack navigates to /bm/approval', () => {
    const page = setup();
    (page as any).goBack();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/bm/approval');
  });
});
