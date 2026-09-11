import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { SUPERADMIN_URL } from '../../../core/config/url';
import { ApiResponse } from '../../../shared/models/api-response';
import { Role } from '../models/role.model';
import { Menu } from '../models/role.model';

@Injectable({ providedIn: 'root' })
export class MenuServices{
  private readonly api = inject(ApiService);

  getAllMenus(): Observable<ApiResponse<Menu[]>> {
    return this.api.get<Menu[]>(SUPERADMIN_URL.auth.getAllMenus);
  }

  updateMenu(id: number, name: string, url: string, logo: string): Observable<ApiResponse<unknown>> {
    return this.api.put<unknown>(SUPERADMIN_URL.auth.updateMenu(id), { name, url, logo });
  }
  deleteMenu(id: number): Observable<ApiResponse<unknown>> {
    return this.api.delete<unknown>(SUPERADMIN_URL.auth.deleteMenu(id));
  }
  saveMenu(name: string, url: string, logo: string): Observable<ApiResponse<unknown>> {
    return this.api.post<unknown>(SUPERADMIN_URL.auth.saveMenu, { name, url, logo });
  }
}
