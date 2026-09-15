import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';
import { ApprovalDetailPage } from './approval-detail';
import { AuthStateService, UserProfile } from '../../../../../core/services/auth-state.service';
import { ApprovalApiService } from '../../../infrastructure/approval-api.service';
import { LimitApplicationDetail } from '../../../domain/entities/limit-application';

function detail(overrides: Partial<LimitApplicationDetail> = {}): LimitApplicationDetail {
  return {
    id: 1,
    applicationCode: 'A1',
    status: 'PENDING_CHECKER',
    customerIdentity: '123',
    customerName: 'Budi',
    customerAddress: null,
    customerAddress2: null,
    branchName: null,
    provinceName: null,
    regencyName: null,
    districtName: null,
    villageName: null,
    hasFotoKtp: false,
    hasFotoKyc: false,
    incomeAmount: null,
    vida: null,
    pefindo: null,
    positiveAppsCount: 0,
    pinjolAppsCount: 0,
    judolAppsCount: 0,
    bankingAppsCount: 0,
    engineScore: null,
    engineRiskCategory: null,
    engineRecommendation: null,
    engineKeyFactors: [],
    engineSuggestionLimit: 1_000_000,
    checkerLimitMin: null,
    checkerLimitMax: null,
    checkerRecommendation: null,
    checkerPurposeLimit: null,
    checkerReason: null,
    finalApprovedLimit: null,
    bmReason: null,
    createdAt: '2026-01-01',
    lockedByIdentity: null,
    lockedByName: null,
    lockedByMe: false,
    canReview: true,
    ...overrides
  };
}

describe('ApprovalDetailPage', () => {
  let userProfileSubject: BehaviorSubject<UserProfile | null>;
  let approvalApi: {
    getDetail: ReturnType<typeof vi.fn>;
    getPicture: ReturnType<typeof vi.fn>;
    checkerDecide: ReturnType<typeof vi.fn>;
    bmDecide: ReturnType<typeof vi.fn>;
  };
  let router: Router;

  function setup(role: string, detailResult: Observable<LimitApplicationDetail> = of(detail())) {
    userProfileSubject = new BehaviorSubject<UserProfile | null>({ identity: '1', name: 'A', role });
    approvalApi = {
      getDetail: vi.fn().mockReturnValue(detailResult),
      getPicture: vi.fn().mockReturnValue(of(new Blob(['x']))),
      checkerDecide: vi.fn().mockReturnValue(of(undefined)),
      bmDecide: vi.fn().mockReturnValue(of(undefined))
    };

    TestBed.configureTestingModule({
      providers: [
        ApprovalDetailPage,
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: new Map([['id', '1']]) } } },
        { provide: AuthStateService, useValue: { userProfile$: userProfileSubject.asObservable() } },
        { provide: ApprovalApiService, useValue: approvalApi }
      ]
    });
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    return TestBed.inject(ApprovalDetailPage);
  }

  beforeEach(() => {
    if (!(globalThis as any).URL.createObjectURL) (globalThis as any).URL.createObjectURL = () => '';
    if (!(globalThis as any).URL.revokeObjectURL) (globalThis as any).URL.revokeObjectURL = () => {};
    vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:mock-url');
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {});
  });

  it('[positive] loads the detail and populates the signal', () => {
    const page = setup('CHECKER');
    expect((page as any).detail()).toEqual(detail());
    expect((page as any).loading()).toBe(false);
  });

  it('[negative] load error sets errorMessage and stops loading', () => {
    const page = setup('CHECKER', throwError(() => new Error('Gagal memuat detail khusus.')));
    expect((page as any).errorMessage()).toBe('Gagal memuat detail khusus.');
    expect((page as any).loading()).toBe(false);
  });

  it('[positive] fetches both photos as object URLs when both flags are true', () => {
    const page = setup('CHECKER', of(detail({ hasFotoKtp: true, hasFotoKyc: true })));
    expect(approvalApi.getPicture).toHaveBeenCalledWith(1, 'ktp');
    expect(approvalApi.getPicture).toHaveBeenCalledWith(1, 'kyc');
    expect((page as any).ktpPhotoUrl()).toBe('blob:mock-url');
    expect((page as any).kycPhotoUrl()).toBe('blob:mock-url');
  });

  it('[negative] does not fetch photos when both flags are false', () => {
    setup('CHECKER', of(detail({ hasFotoKtp: false, hasFotoKyc: false })));
    expect(approvalApi.getPicture).not.toHaveBeenCalled();
  });

  it('[negative] a photo fetch error sets photoError', () => {
    userProfileSubject = new BehaviorSubject<UserProfile | null>({ identity: '1', name: 'A', role: 'CHECKER' });
    approvalApi = {
      getDetail: vi.fn().mockReturnValue(of(detail({ hasFotoKtp: true }))),
      getPicture: vi.fn().mockReturnValue(throwError(() => new Error('fail'))),
      checkerDecide: vi.fn(),
      bmDecide: vi.fn()
    };
    TestBed.configureTestingModule({
      providers: [
        ApprovalDetailPage,
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: new Map([['id', '1']]) } } },
        { provide: AuthStateService, useValue: { userProfile$: userProfileSubject.asObservable() } },
        { provide: ApprovalApiService, useValue: approvalApi }
      ]
    });
    const page = TestBed.inject(ApprovalDetailPage);
    expect((page as any).photoError()).toBe('Gagal memuat foto KTP/selfie.');
  });

  it('[positive] isNegative is true only when engineRecommendation is REJECTED', () => {
    const page = setup('CHECKER', of(detail({ engineRecommendation: 'REJECTED' })));
    expect((page as any).isNegative()).toBe(true);
  });

  it('[negative] isNegative is false for other recommendations', () => {
    const page = setup('CHECKER', of(detail({ engineRecommendation: 'APPROVED' })));
    expect((page as any).isNegative()).toBe(false);
  });

  it('[positive] canDecideStatus is true for BM at PENDING_BM', () => {
    const page = setup('BM', of(detail({ status: 'PENDING_BM' })));
    expect((page as any).canDecideStatus()).toBe(true);
  });

  it('[negative] canDecideStatus is false for BM at a different status', () => {
    const page = setup('BM', of(detail({ status: 'PENDING_CHECKER' })));
    expect((page as any).canDecideStatus()).toBe(false);
  });

  it('[positive] canDecideStatus is true for Checker at PENDING_CHECKER', () => {
    const page = setup('CHECKER', of(detail({ status: 'PENDING_CHECKER' })));
    expect((page as any).canDecideStatus()).toBe(true);
  });

  it('[negative] canDecideStatus is false for Checker at a different status', () => {
    const page = setup('CHECKER', of(detail({ status: 'PENDING_BM' })));
    expect((page as any).canDecideStatus()).toBe(false);
  });

  it('[positive] bmMaxLimit uses checkerPurposeLimit as baseline plus 2,000,000', () => {
    const page = setup('BM', of(detail({ checkerPurposeLimit: 5_000_000, engineSuggestionLimit: 1_000_000 })));
    expect((page as any).bmMaxLimit()).toBe(7_000_000);
  });

  it('[positive] bmMaxLimit falls back to engineSuggestionLimit when checkerPurposeLimit is null', () => {
    const page = setup('BM', of(detail({ checkerPurposeLimit: null, engineSuggestionLimit: 1_000_000 })));
    expect((page as any).bmMaxLimit()).toBe(3_000_000);
  });

  it('[positive] BM approve form prefills from checkerPurposeLimit', () => {
    const page = setup('BM', of(detail({ checkerPurposeLimit: 4_000_000, engineSuggestionLimit: 1_000_000 })));
    (page as any).openForm('APPROVE');
    expect((page as any).limitInput()).toBe(4_000_000);
    expect((page as any).formOpen()).toBe(true);
  });

  it('[positive] Checker approve form prefills from engineSuggestionLimit only', () => {
    const page = setup('CHECKER', of(detail({ checkerPurposeLimit: 4_000_000, engineSuggestionLimit: 1_000_000 })));
    (page as any).openForm('APPROVE');
    expect((page as any).limitInput()).toBe(1_000_000);
  });

  it('[negative] reject form does not touch limitInput', () => {
    const page = setup('CHECKER');
    (page as any).limitInput.set(999);
    (page as any).openForm('REJECT');
    expect((page as any).limitInput()).toBe(999);
    expect((page as any).actionType()).toBe('REJECT');
  });

  it('[negative] cancelForm is a no-op while submitting', () => {
    const page = setup('CHECKER');
    (page as any).openForm('REJECT');
    (page as any).submitting.set(true);
    (page as any).cancelForm();
    expect((page as any).formOpen()).toBe(true);
  });

  it('[positive] cancelForm resets state when not submitting', () => {
    const page = setup('CHECKER');
    (page as any).openForm('REJECT');
    (page as any).cancelForm();
    expect((page as any).formOpen()).toBe(false);
    expect((page as any).actionType()).toBeNull();
  });

  it('[negative] confirm APPROVE with a non-positive limit sets an error and does not call the API', () => {
    const page = setup('CHECKER');
    (page as any).openForm('APPROVE');
    (page as any).limitInput.set(0);
    (page as any).confirm();
    expect((page as any).actionError()).toBe('Nominal plafond wajib diisi.');
    expect(approvalApi.checkerDecide).not.toHaveBeenCalled();
  });

  it('[negative] BM confirm APPROVE over bmMaxLimit sets an error', () => {
    const page = setup('BM', of(detail({ checkerPurposeLimit: 1_000_000 })));
    (page as any).openForm('APPROVE');
    (page as any).limitInput.set(5_000_000);
    (page as any).confirm();
    expect((page as any).actionError()).toContain('Plafond final maksimal');
    expect(approvalApi.bmDecide).not.toHaveBeenCalled();
  });

  it('[negative] BM confirm APPROVE with a changed amount but no reason sets an error', () => {
    const page = setup('BM', of(detail({ checkerPurposeLimit: 1_000_000 })));
    (page as any).openForm('APPROVE');
    (page as any).limitInput.set(1_500_000);
    (page as any).confirm();
    expect((page as any).actionError()).toBe('Alasan wajib diisi kalau nominal beda dari plafond Checker.');
  });

  it('[positive] BM confirm APPROVE within cap and matching baseline calls bmDecide with finalLimit', () => {
    const page = setup('BM', of(detail({ checkerPurposeLimit: 1_000_000, status: 'PENDING_BM' })));
    (page as any).openForm('APPROVE');
    (page as any).limitInput.set(1_000_000);
    (page as any).confirm();
    expect(approvalApi.bmDecide).toHaveBeenCalledWith(1, { action: 'APPROVE', finalLimit: 1_000_000 });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/bm/approval');
  });

  it('[negative] Checker confirm APPROVE below checkerLimitMin sets an error', () => {
    const page = setup('CHECKER', of(detail({ checkerLimitMin: 2_000_000 })));
    (page as any).openForm('APPROVE');
    (page as any).limitInput.set(1_000_000);
    (page as any).confirm();
    expect((page as any).actionError()).toContain('Plafond minimal');
    expect(approvalApi.checkerDecide).not.toHaveBeenCalled();
  });

  it('[negative] Checker confirm APPROVE above checkerLimitMax sets an error', () => {
    const page = setup('CHECKER', of(detail({ checkerLimitMax: 2_000_000 })));
    (page as any).openForm('APPROVE');
    (page as any).limitInput.set(3_000_000);
    (page as any).confirm();
    expect((page as any).actionError()).toContain('Plafond maksimal');
  });

  it('[positive] Checker confirm APPROVE within bounds calls checkerDecide with purposeLimit', () => {
    const page = setup('CHECKER', of(detail({ checkerLimitMin: 500_000, checkerLimitMax: 2_000_000, status: 'PENDING_CHECKER' })));
    (page as any).openForm('APPROVE');
    (page as any).limitInput.set(1_000_000);
    (page as any).confirm();
    expect(approvalApi.checkerDecide).toHaveBeenCalledWith(1, { action: 'APPROVE', purposeLimit: 1_000_000 });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/approval');
  });

  it('[negative] confirm REJECT without a reason sets an error and does not call the API', () => {
    const page = setup('CHECKER');
    (page as any).openForm('REJECT');
    (page as any).confirm();
    expect((page as any).actionError()).toBe('Alasan reject wajib diisi.');
    expect(approvalApi.checkerDecide).not.toHaveBeenCalled();
  });

  it('[positive] confirm REJECT with a reason calls checkerDecide and navigates back', () => {
    const page = setup('CHECKER');
    (page as any).openForm('REJECT');
    (page as any).reasonInput.set('tidak memenuhi syarat');
    (page as any).confirm();
    expect(approvalApi.checkerDecide).toHaveBeenCalledWith(1, { action: 'REJECT', reason: 'tidak memenuhi syarat' });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/approval');
  });

  it('[negative] confirm API error sets actionError and resets submitting', () => {
    const page = setup('CHECKER');
    approvalApi.checkerDecide.mockReturnValue(throwError(() => new Error('Gagal menyimpan keputusan khusus.')));
    (page as any).openForm('REJECT');
    (page as any).reasonInput.set('alasan');
    (page as any).confirm();
    expect((page as any).actionError()).toBe('Gagal menyimpan keputusan khusus.');
    expect((page as any).submitting()).toBe(false);
  });

  it('[negative] confirm is a no-op without an actionType set', () => {
    const page = setup('CHECKER');
    (page as any).confirm();
    expect(approvalApi.checkerDecide).not.toHaveBeenCalled();
  });

  it('[positive] goBack navigates to /approval for a Checker', () => {
    const page = setup('CHECKER');
    (page as any).goBack();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/approval');
  });

  it('[positive] goBack navigates to /bm/approval for a BM', () => {
    const page = setup('BM');
    (page as any).goBack();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/bm/approval');
  });
});
