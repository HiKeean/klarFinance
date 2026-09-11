import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { SUPERADMIN_URL } from '../../../core/config/url';
import { ApiResponse } from '../../../shared/models/api-response';
import { RoleMenu } from '../models/role.model';

export interface RoleMenuFilter {
  menuName?: string;
  menuUrl?: string;
  role?: string;
}

@Injectable({ providedIn: 'root' })
export class RoleMenuService {
  private readonly api = inject(ApiService);

  getAllRoleMenus(filter?: RoleMenuFilter): Observable<ApiResponse<RoleMenu[]>> {
    const params: Record<string, string> = {};
    if (filter?.menuName) params['menuName'] = filter.menuName;
    if (filter?.menuUrl) params['menuUrl'] = filter.menuUrl;
    if (filter?.role) params['role'] = filter.role;
    return this.api.get<RoleMenu[]>(SUPERADMIN_URL.auth.getAllRoleMenus, params);
  }

  addRoleMenu(role: string, menu: string): Observable<ApiResponse<unknown>> {
    return this.api.post<unknown>(SUPERADMIN_URL.auth.addRoleMenu, { role, menu });
  }

  updateRoleMenu(id: number, role: string, menu: string): Observable<ApiResponse<unknown>> {
    return this.api.put<unknown>(SUPERADMIN_URL.auth.updateRoleMenu(id), { role, menu });
  }

  deleteRoleMenu(id: number): Observable<ApiResponse<unknown>> {
    return this.api.delete<unknown>(SUPERADMIN_URL.auth.deleteRoleMenu(id));
  }
}
