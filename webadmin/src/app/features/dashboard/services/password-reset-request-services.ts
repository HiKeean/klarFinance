import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { SUPERADMIN_URL } from '../../../core/config/url';
import { ApiResponse } from '../../../shared/models/api-response';
import { PasswordResetRequestItem } from '../models/password-reset-request.model';

@Injectable({ providedIn: 'root' })
export class PasswordResetRequestServices {
  private readonly api = inject(ApiService);

  getAll(status = 'PENDING'): Observable<ApiResponse<PasswordResetRequestItem[]>> {
    return this.api.get<PasswordResetRequestItem[]>(SUPERADMIN_URL.auth.passwordResetRequests, { status });
  }

  decide(id: number, action: 'APPROVE' | 'REJECT', reason?: string): Observable<ApiResponse<unknown>> {
    return this.api.post<unknown>(SUPERADMIN_URL.auth.passwordResetRequestDecision(id), { action, reason });
  }
}
