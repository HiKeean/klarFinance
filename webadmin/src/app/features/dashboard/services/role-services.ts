import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { SUPERADMIN_URL } from '../../../core/config/url';
import { ApiResponse } from '../../../shared/models/api-response';
import { Role } from '../models/role.model';

@Injectable({ providedIn: 'root' })
export class RoleServices {
  private readonly api = inject(ApiService);

  getAllRoles(): Observable<ApiResponse<Role[]>> {
    return this.api.get<Role[]>(SUPERADMIN_URL.auth.getAllRoles);
  }

  updateRole(id: number, role: string): Observable<ApiResponse<unknown>> {
    return this.api.put<unknown>(SUPERADMIN_URL.auth.updateRole(id), { role });
  }

  deleteRole(id: number): Observable<ApiResponse<unknown>> {
    return this.api.delete<unknown>(SUPERADMIN_URL.auth.deleteRole(id));
  }

  saveRole(role: string): Observable<ApiResponse<unknown>> {
    return this.api.post<unknown>(SUPERADMIN_URL.auth.saveRole, { role });
  }
}
