import { TestBed } from '@angular/core/testing';
import { BehaviorSubject, of, Subject, throwError } from 'rxjs';
import { DashboardPage } from './dashboard';
import { AuthStateService, UserProfile } from '../../../../../core/services/auth-state.service';
import { DashboardApiService } from '../../../infrastructure/dashboard-api.service';
import { DashboardSummary } from '../../../domain/entities/dashboard-summary';

function summary(overrides: Partial<DashboardSummary> = {}): DashboardSummary {
  return {
    needsToReview: 5,
    loansInRegion: 1_000_000,
    activeBorrowers: 10,
    delinquentBorrowers: 2,
    loanStatusDistribution: [
      { label: 'Current', percent: 70 },
      { label: 'Overdue', percent: 30 }
    ],
    nplPercent: 5.5,
    nplSeverity: 'YELLOW',
    totalOutstandingPenalty: 200_000,
    ...overrides
  };
}

describe('DashboardPage', () => {
  let userProfileSubject: BehaviorSubject<UserProfile | null>;
  let dashboardApi: { getSummary: ReturnType<typeof vi.fn> };

  function setup(role: string, result = of(summary())) {
    userProfileSubject = new BehaviorSubject<UserProfile | null>({ identity: '1', name: 'A', role });
    dashboardApi = { getSummary: vi.fn().mockReturnValue(result) };
    TestBed.configureTestingModule({
      providers: [
        DashboardPage,
        { provide: AuthStateService, useValue: { userProfile$: userProfileSubject.asObservable(), menuList$: of([]) } },
        { provide: DashboardApiService, useValue: dashboardApi }
      ]
    });
    const page = TestBed.inject(DashboardPage);
    TestBed.tick();
    return page;
  }

  afterEach(() => {
    TestBed.resetTestingModule();
  });

  it('[negative] a non-BM role never calls getSummary', () => {
    setup('CHECKER');
    expect(dashboardApi.getSummary).not.toHaveBeenCalled();
  });

  it('[positive] a BM role fetches the summary exactly once', () => {
    setup('BM');
    expect(dashboardApi.getSummary).toHaveBeenCalledTimes(1);
    TestBed.tick();
    expect(dashboardApi.getSummary).toHaveBeenCalledTimes(1);
  });

  it('[negative] a fetch error resets loading without setting summary', () => {
    const page = setup('BM', throwError(() => new Error('fail')));
    expect((page as any).loading()).toBe(false);
    expect((page as any).summary()).toBeNull();
  });

  it('[positive] loansInRegion and totalOutstandingPenalty format as Rupiah', () => {
    const page = setup('BM', of(summary({ loansInRegion: 1_500_000, totalOutstandingPenalty: 75_000 })));
    expect((page as any).loansInRegion()).toBe('Rp 1.500.000');
    expect((page as any).totalOutstandingPenalty()).toBe('Rp 75.000');
  });

  it('[negative] loansInRegion and totalOutstandingPenalty are null when the source value is null', () => {
    const page = setup('BM', of(summary({ loansInRegion: null, totalOutstandingPenalty: null })));
    expect((page as any).loansInRegion()).toBeNull();
    expect((page as any).totalOutstandingPenalty()).toBeNull();
  });

  it('[negative] donutBackground falls back to a flat color for an empty slice list', () => {
    const page = setup('BM', of(summary({ loanStatusDistribution: [] })));
    expect((page as any).donutBackground()).toBe('#e6ebef');
  });

  it('[positive] donutBackground builds a conic-gradient with correct deg boundaries for known labels', () => {
    const page = setup(
      'BM',
      of(
        summary({
          loanStatusDistribution: [
            { label: 'Current', percent: 60 },
            { label: 'Overdue', percent: 40 }
          ]
        })
      )
    );
    const bg = (page as any).donutBackground();
    expect(bg).toContain('conic-gradient(');
    expect(bg).toContain('var(--slice-current) 0deg 216deg');
    expect(bg).toContain('var(--slice-overdue) 216deg 360deg');
  });

  it('[negative] donutBackground uses the fallback color for an unknown label', () => {
    const page = setup('BM', of(summary({ loanStatusDistribution: [{ label: 'Weird', percent: 100 }] })));
    expect((page as any).donutBackground()).toContain('var(--slice-fallback)');
  });

  it('[negative] sliceColor falls back for an unknown label', () => {
    const page = setup('BM');
    expect((page as any).sliceColor('Unknown')).toBe('var(--slice-fallback)');
    expect((page as any).sliceColor('Current')).toBe('var(--slice-current)');
  });

  it('[positive] nplSeverityLabel maps GREEN to "Sehat"', () => {
    const page = setup('BM', of(summary({ nplSeverity: 'GREEN' })));
    expect((page as any).nplSeverityLabel()).toBe('Sehat');
  });

  it('[positive] nplSeverityLabel maps RED to "Kritis"', () => {
    const page = setup('BM', of(summary({ nplSeverity: 'RED' })));
    expect((page as any).nplSeverityLabel()).toBe('Kritis');
  });

  it('[negative] nplSeverityLabel is null when severity is null', () => {
    const page = setup('BM', of(summary({ nplSeverity: null })));
    expect((page as any).nplSeverityLabel()).toBeNull();
  });

  it('[positive] nplSeverityClass includes the severity-specific class', () => {
    const page = setup('BM', of(summary({ nplSeverity: 'RED' })));
    expect((page as any).nplSeverityClass()).toBe('npl-badge npl-red');
  });

  it('[negative] nplSeverityClass falls back to the bare badge when severity is null', () => {
    const page = setup('BM', of(summary({ nplSeverity: null })));
    expect((page as any).nplSeverityClass()).toBe('npl-badge');
  });

  it('[negative] a still-loading fetch does not trigger a second concurrent call', () => {
    const neverCompletes = new Subject<DashboardSummary>();
    setup('BM', neverCompletes as any);
    TestBed.tick();
    expect(dashboardApi.getSummary).toHaveBeenCalledTimes(1);
  });
});
