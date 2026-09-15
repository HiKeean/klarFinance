import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { BehaviorSubject, Subject, of, throwError } from 'rxjs';
import { ApprovalListPage } from './approval-list';
import { AuthStateService, UserProfile } from '../../../../../core/services/auth-state.service';
import { ApprovalApiService } from '../../../infrastructure/approval-api.service';
import { CheckerRealtimeService, AssignmentNotification } from '../../../../../core/services/checker-realtime.service';
import { LimitApplicationSummary } from '../../../domain/entities/limit-application';

function item(overrides: Partial<LimitApplicationSummary> = {}): LimitApplicationSummary {
  return {
    type: 'LIMIT_APPLICATION',
    id: 1,
    customerIdentity: '123',
    customerName: 'Budi',
    status: 'PENDING_CHECKER',
    createdAt: '2026-01-01',
    incomeAmount: null,
    engineSuggestionLimit: null,
    checkerPurposeLimit: null,
    finalApprovedLimit: null,
    requestedAmount: null,
    tenorMonths: null,
    usedLimitSnapshot: null,
    totalLimitSnapshot: null,
    utilizationPercent: null,
    pefindoScore: null,
    pefindoColStatus: null,
    lockedByIdentity: null,
    lockedByName: null,
    lockedByMe: false,
    ...overrides
  };
}

describe('ApprovalListPage', () => {
  let userProfileSubject: BehaviorSubject<UserProfile | null>;
  let notifications: Subject<AssignmentNotification>;
  let connected: Subject<void>;
  let approvalApi: { getQueue: ReturnType<typeof vi.fn>; checkerBreak: ReturnType<typeof vi.fn> };
  let realtime: { connect: ReturnType<typeof vi.fn>; disconnect: ReturnType<typeof vi.fn>; notifications$: Subject<AssignmentNotification>; connected$: Subject<void> };
  let router: Router;

  function setup(role: string, queueResult = of([item()])) {
    userProfileSubject = new BehaviorSubject<UserProfile | null>({ identity: '1', name: 'A', role });
    notifications = new Subject<AssignmentNotification>();
    connected = new Subject<void>();
    approvalApi = { getQueue: vi.fn().mockReturnValue(queueResult), checkerBreak: vi.fn() };
    realtime = { connect: vi.fn().mockResolvedValue(undefined), disconnect: vi.fn(), notifications$: notifications, connected$: connected };

    TestBed.configureTestingModule({
      providers: [
        ApprovalListPage,
        provideRouter([]),
        { provide: AuthStateService, useValue: { userProfile$: userProfileSubject.asObservable() } },
        { provide: ApprovalApiService, useValue: approvalApi },
        { provide: CheckerRealtimeService, useValue: realtime }
      ]
    });
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    return TestBed.inject(ApprovalListPage);
  }

  it('[positive] BM role loads the queue but does not auto-navigate even with items', () => {
    const page = setup('BM');
    expect(approvalApi.getQueue).toHaveBeenCalled();
    expect((page as any).items()).toEqual([item()]);
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('[positive] Checker role auto-navigates to the first queued item', () => {
    const page = setup('CHECKER', of([item({ id: 77 })]));
    expect(router.navigate).toHaveBeenCalledWith(['/approval', 77]);
    expect((page as any).isBm()).toBe(false);
  });

  it('[positive] Checker role connects realtime exactly once via the effect', () => {
    setup('CHECKER');
    TestBed.tick();
    expect(realtime.connect).toHaveBeenCalledTimes(1);
    TestBed.tick();
    expect(realtime.connect).toHaveBeenCalledTimes(1);
  });

  it('[negative] load() error sets errorMessage and stops loading', () => {
    const page = setup('BM', throwError(() => new Error('Gagal memuat antrean khusus.')));
    expect((page as any).errorMessage()).toBe('Gagal memuat antrean khusus.');
    expect((page as any).loading()).toBe(false);
  });

  it('[negative] load() error without a message falls back to a generic message', () => {
    const page = setup('BM', throwError(() => ({})));
    expect((page as any).errorMessage()).toBe('Gagal memuat antrean.');
  });

  it('[positive] a realtime notification sets the notice and reloads the queue', () => {
    const page = setup('BM');
    approvalApi.getQueue.mockClear();
    notifications.next({ type: 'ASSIGNED', applicationId: 1, message: 'Ada aplikasi baru' });
    expect((page as any).realtimeNotice()).toBe('Ada aplikasi baru');
    expect(approvalApi.getQueue).toHaveBeenCalled();
  });

  it('[positive] a realtime (re)connect event reloads the queue', () => {
    setup('BM');
    approvalApi.getQueue.mockClear();
    connected.next();
    expect(approvalApi.getQueue).toHaveBeenCalled();
  });

  it('[positive] takeBreak success disconnects realtime and navigates to the dashboard', () => {
    const page = setup('BM');
    approvalApi.checkerBreak.mockReturnValue(of(undefined));
    (page as any).takeBreak();
    expect(realtime.disconnect).toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/dashboard');
  });

  it('[negative] takeBreak error resets the breaking flag and sets errorMessage', () => {
    const page = setup('BM');
    approvalApi.checkerBreak.mockReturnValue(throwError(() => new Error('Gagal mengambil istirahat khusus.')));
    (page as any).takeBreak();
    expect((page as any).breaking()).toBe(false);
    expect((page as any).errorMessage()).toBe('Gagal mengambil istirahat khusus.');
  });

  it('[negative] takeBreak is a no-op while already breaking', () => {
    const page = setup('BM');
    approvalApi.checkerBreak.mockReturnValue(new Subject()); // never resolves -> stays "breaking"
    (page as any).takeBreak();
    approvalApi.checkerBreak.mockClear();
    (page as any).takeBreak();
    expect(approvalApi.checkerBreak).not.toHaveBeenCalled();
  });

  it('[negative] openDetail does nothing when locked by another user', () => {
    const page = setup('BM');
    (page as any).openDetail(item({ lockedByIdentity: '999', lockedByMe: false }));
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('[positive] openDetail navigates to the loan-review page for a LOAN_REVIEW item', () => {
    const page = setup('BM');
    (page as any).openDetail(item({ id: 5, type: 'LOAN_REVIEW' }));
    expect(router.navigate).toHaveBeenCalledWith(['/bm/loan-review', 5]);
  });

  it('[positive] openDetail navigates to the approval detail page for a LIMIT_APPLICATION item', () => {
    const page = setup('BM');
    (page as any).openDetail(item({ id: 6, type: 'LIMIT_APPLICATION' }));
    expect(router.navigate).toHaveBeenCalledWith(['/bm/approval', 6]);
  });

  it('[positive] openDetail still navigates when the item is locked by the current user', () => {
    const page = setup('BM');
    (page as any).openDetail(item({ id: 8, lockedByIdentity: '1', lockedByMe: true }));
    expect(router.navigate).toHaveBeenCalledWith(['/bm/approval', 8]);
  });
});
