export type NplSeverity = 'GREEN' | 'YELLOW' | 'RED';

export interface NplReportItem {
  branchId: number;
  branchName: string;
  activeLoanCount: number;
  overdueLoanCount: number;
  nplPercent: number;
  nplSeverity: NplSeverity;
  totalOutstandingPenalty: number;
  regencyIds: number[];
}
