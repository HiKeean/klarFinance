export interface LoanStatusSlice {
  label: string;
  percent: number;
}

export interface DashboardSummary {
  needsToReview: number;
  // Null buat Checker — cuma diisi backend kalau caller-nya BM.
  loansInRegion: number | null;
  activeBorrowers: number | null;
  delinquentBorrowers: number | null;
  loanStatusDistribution: LoanStatusSlice[] | null;
  // NPL branch BM sendiri - null buat Checker, sama kayak field di atas.
  nplPercent: number | null;
  nplSeverity: 'GREEN' | 'YELLOW' | 'RED' | null;
  totalOutstandingPenalty: number | null;
}
