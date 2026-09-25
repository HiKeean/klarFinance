import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { SUPERADMIN_URL } from '../../../core/config/url';
import { ApiResponse } from '../../../shared/models/api-response';
import { BranchLoanPageResponse, NplReportItem, StartCallResponse } from '../models/npl-report.model';

@Injectable({ providedIn: 'root' })
export class NplReportServices {
  private readonly api = inject(ApiService);

  getReport(): Observable<ApiResponse<NplReportItem[]>> {
    return this.api.get<NplReportItem[]>(SUPERADMIN_URL.dbo.nplReport);
  }

  getBranchLoans(branchId: number, page: number, size: number, search = ''): Observable<ApiResponse<BranchLoanPageResponse>> {
    const params: Record<string, string | number | boolean> = { page, size };
    if (search) params['search'] = search;
    return this.api.get<BranchLoanPageResponse>(SUPERADMIN_URL.dbo.nplReportBranchLoans(branchId), params);
  }

  /** Tombol Call (demo): backend meminta deskcall membuat panggilan lalu mengirim push ke HP nasabah. */
  startCall(loanId: number): Observable<ApiResponse<StartCallResponse>> {
    return this.api.post<StartCallResponse>(SUPERADMIN_URL.deskcall.startCall(loanId));
  }
}
