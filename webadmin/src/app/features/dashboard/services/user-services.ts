import { inject, Injectable } from '@angular/core';
import { ApiService } from '../../../core/services/api.service';
import { Observable } from 'rxjs';
import { ApiResponse } from '../../../shared/models/api-response';
import { UserPageResponse } from '../models/user.model';
import { SUPERADMIN_URL } from '../../../core/config/url';
import { RegisterEmployeeRequest } from '../models/registration.model';

@Injectable({
  providedIn: 'root',
})
export class UserServices {
  private readonly api = inject(ApiService);

  getData(page: number, size: number, search = '', role = ''): Observable<ApiResponse<UserPageResponse>> {
    const params: Record<string, string | number | boolean> = { page, size };
    if (search) params['search'] = search;
    if (role) params['role'] = role;
    return this.api.get<UserPageResponse>(SUPERADMIN_URL.auth.getAllUsers, params);
  }

  registerEmployee(payload: RegisterEmployeeRequest): Observable<ApiResponse<unknown>> {
    return this.api.post<unknown>(SUPERADMIN_URL.auth.registration, payload);
  }

  deleteRegistration(identity: string): Observable<ApiResponse<unknown>> {
    return this.api.delete<unknown>(SUPERADMIN_URL.auth.deleteRegistration, { identity });
  }

}
