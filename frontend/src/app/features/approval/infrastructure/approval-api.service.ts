import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { INTERNAL_URL } from '../../../core/config/url';
import {
  DecisionRequest,
  LimitApplicationDetail,
  LimitApplicationSummary,
  LoanReviewDecisionRequest,
  LoanReviewDetail
} from '../domain/entities/limit-application';

@Injectable({ providedIn: 'root' })
export class ApprovalApiService {
  private readonly api = inject(ApiService);

  getQueue(): Observable<LimitApplicationSummary[]> {
    return this.api.get<LimitApplicationSummary[]>(INTERNAL_URL.los.limitApplications).pipe(
      map((response) => {
        if (!response.success) throw new Error(response.message);
        return response.data;
      })
    );
  }

  getDetail(id: number): Observable<LimitApplicationDetail> {
    return this.api.get<LimitApplicationDetail>(INTERNAL_URL.los.limitApplicationDetail(id)).pipe(
      map((response) => {
        if (!response.success) throw new Error(response.message);
        return response.data;
      })
    );
  }

  /** Blob mentah (bukan ApiResponse envelope) - lihat ApiService.getBlob. */
  getPicture(id: number, type: 'ktp' | 'kyc'): Observable<Blob> {
    return this.api.getBlob(INTERNAL_URL.los.limitApplicationPicture(id, type));
  }

  checkerDecide(id: number, request: DecisionRequest): Observable<void> {
    return this.api.post<unknown>(INTERNAL_URL.los.checkerDecision(id), request).pipe(
      map((response) => {
        if (!response.success) throw new Error(response.message);
      })
    );
  }

  bmDecide(id: number, request: DecisionRequest): Observable<void> {
    return this.api.post<unknown>(INTERNAL_URL.los.bmDecision(id), request).pipe(
      map((response) => {
        if (!response.success) throw new Error(response.message);
      })
    );
  }

  checkerBreak(): Observable<void> {
    return this.api.post<unknown>(INTERNAL_URL.los.checkerBreak, {}).pipe(
      map((response) => {
        if (!response.success) throw new Error(response.message);
      })
    );
  }

  getLoanReviewDetail(id: number): Observable<LoanReviewDetail> {
    return this.api.get<LoanReviewDetail>(INTERNAL_URL.fin.loanReviewDetail(id)).pipe(
      map((response) => {
        if (!response.success) throw new Error(response.message);
        return response.data;
      })
    );
  }

  loanReviewBmDecide(id: number, request: LoanReviewDecisionRequest): Observable<void> {
    return this.api.post<unknown>(INTERNAL_URL.fin.loanReviewBmDecision(id), request).pipe(
      map((response) => {
        if (!response.success) throw new Error(response.message);
      })
    );
  }
}
