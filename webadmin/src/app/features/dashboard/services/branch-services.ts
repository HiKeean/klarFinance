import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { DBO_URL, SUPERADMIN_URL } from '../../../core/config/url';
import { ApiResponse } from '../../../shared/models/api-response';
import { BmOption, BranchListItem } from '../models/branch.model';

/** Mirrors backend ApiResponsePagination<GetAllBranchResponse> - DBO_URL.branch is paginated. */
interface BranchPage {
  content: {
    branchCode: number;
    name: string;
    address: string | null;
    village: { name: string } | null;
  }[];
}

/** Mirrors backend ApiResponsePagination<GetAllSuperadminResponse> - subset used here. */
interface UserPage {
  content: {
    identity: string | null;
    name: string;
    branch: { branchCode: number; name: string } | null;
  }[];
}

@Injectable({ providedIn: 'root' })
export class BranchServices {
  private readonly api = inject(ApiService);

  /** Gabungin GET /admin/branch (daftar branch) + GET /admin/auth/users?role=BM (siapa BM-nya) -
   * gak ada endpoint gabungan di backend, jadi di-join di client. */
  listBranches(): Observable<BranchListItem[]> {
    return this.listBranchesRaw().pipe(
      map((branches) => branches.map((branch) => ({ ...branch, bmIdentity: null, bmName: null })))
    );
  }

  private listBranchesRaw(): Observable<Omit<BranchListItem, 'bmIdentity' | 'bmName'>[]> {
    return this.api.get<BranchPage>(DBO_URL.branch, { page: 0, size: 200 }).pipe(
      map((response) => {
        if (!response.success) throw new Error(response.message);
        return response.data.content.map((b) => ({
          branchCode: b.branchCode,
          name: b.name,
          address: b.address,
          villageName: b.village?.name ?? null,
        }));
      })
    );
  }

  listBms(): Observable<BmOption[]> {
    const params = { page: 0, size: 500, role: 'BM' };
    return this.api.get<UserPage>(SUPERADMIN_URL.auth.getAllUsers, params).pipe(
      map((response) => {
        if (!response.success) throw new Error(response.message);
        return response.data.content.map((u) => ({
          identity: u.identity ?? '',
          name: u.name,
          branchCode: u.branch?.branchCode ?? null,
          branchName: u.branch?.name ?? null,
        }));
      })
    );
  }

  /** branchCode null = lepas BM ini dari branch manapun. */
  assignBranch(bmIdentity: string, branchCode: number | null): Observable<ApiResponse<unknown>> {
    return this.api.patch<unknown>(SUPERADMIN_URL.auth.assignBranch(bmIdentity), { branchId: branchCode });
  }
}
