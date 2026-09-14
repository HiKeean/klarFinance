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

export type LoanHealthStatus = 'Current' | 'Overdue' | 'Lunas';

export interface BranchLoanDetailItem {
  loanId: number;
  nasabahName: string;
  loanAmount: number;
  status: LoanHealthStatus;
  daysOverdue: number;
}

export interface BranchLoanPage {
  content: BranchLoanDetailItem[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
}

export interface BranchLoanPageResponse {
  branchId: number;
  branchName: string;
  totalAssets: number;
  activeBorrowers: number;
  loans: BranchLoanPage;
}
