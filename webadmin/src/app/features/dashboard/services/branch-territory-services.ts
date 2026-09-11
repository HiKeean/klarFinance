import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { SUPERADMIN_URL, DBO_URL } from '../../../core/config/url';
import { ApiResponse } from '../../../shared/models/api-response';
import { BranchOption, BranchTerritory, RegencyGap } from '../models/branch-territory.model';

/** Mirrors backend ApiResponsePagination<GetAllBranchResponse> - DBO_URL.branch is paginated. */
interface BranchPage {
  content: { branchCode: number; name: string }[];
}

@Injectable({ providedIn: 'root' })
export class BranchTerritoryServices {
  private readonly api = inject(ApiService);

  getAll(branchId?: number): Observable<ApiResponse<BranchTerritory[]>> {
    const params = branchId ? { branchId } : undefined;
    return this.api.get<BranchTerritory[]>(SUPERADMIN_URL.dbo.branchTerritory, params);
  }

  getGaps(): Observable<ApiResponse<RegencyGap[]>> {
    return this.api.get<RegencyGap[]>(SUPERADMIN_URL.dbo.branchTerritoryGaps);
  }

  assignRegency(branchId: number, regencyId: number): Observable<ApiResponse<BranchTerritory>> {
    return this.api.post<BranchTerritory>(SUPERADMIN_URL.dbo.assignRegencyTerritory, { branchId, regencyId });
  }

  assignProvince(branchId: number, provinceId: number): Observable<ApiResponse<BranchTerritory[]>> {
    return this.api.post<BranchTerritory[]>(SUPERADMIN_URL.dbo.assignProvinceTerritory, { branchId, provinceId });
  }

  unassign(id: number): Observable<ApiResponse<unknown>> {
    return this.api.delete<unknown>(SUPERADMIN_URL.dbo.deleteBranchTerritory(id));
  }

  /** DBO_URL.branch itu paginated di backend - jangan pakai Dbo.getAllBranches() (typed salah, flat array padahal isinya {content:[...]}). */
  listBranches(name = ''): Observable<BranchOption[]> {
    const params: Record<string, string | number> = { page: 0, size: 200 };
    if (name) params['name'] = name;
    return this.api.get<BranchPage>(DBO_URL.branch, params).pipe(
      map((response) => {
        if (!response.success) throw new Error(response.message);
        return response.data.content.map((b) => ({ id: b.branchCode, name: b.name }));
      })
    );
  }
}
