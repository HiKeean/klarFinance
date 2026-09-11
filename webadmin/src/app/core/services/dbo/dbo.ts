import { inject, Injectable } from '@angular/core';
import { BranchResponse, DistrictsResponse, ProvinceResponse, RegenciesResponse, VillagesResponse } from '../../../shared/models/dbo-response';
import { ApiResponse } from '../../../shared/models/api-response';
import { Observable } from 'rxjs';
import { ApiService } from '../api.service';
import { DBO_URL } from '../../config/url';

@Injectable({
  providedIn: 'root',
})
export class Dbo {
  private readonly api = inject(ApiService);

  getAllProvinces(search = ''): Observable<ApiResponse<ProvinceResponse[]>> {
    const params: Record<string, string | number | boolean> = {};

    if (search) params['search'] = search;

    return this.api.get<ProvinceResponse[]>(DBO_URL.location.province, params);
  }

  getAllBranches(search = ''): Observable<ApiResponse<BranchResponse[]>> {
    const params: Record<string, string | number | boolean> = {};
    if (search) params['search'] = search;
    return this.api.get<BranchResponse[]>(DBO_URL.branch, params);
  }

  getAllRegencies(provinceId: number, search = ''): Observable<ApiResponse<RegenciesResponse[]>> {
    const params: Record<string, string | number | boolean> = {
      provinceId,
    };

    if (search) params['search'] = search;

    return this.api.get<RegenciesResponse[]>(DBO_URL.location.regency, params);
  }

  getAllDistricts(regenciesId: number, search = ''): Observable<ApiResponse<DistrictsResponse[]>> {
    const params: Record<string, string | number | boolean> = {
      regenciesId,
    };

    if (search) params['search'] = search;

    return this.api.get<DistrictsResponse[]>(DBO_URL.location.district, params);
  }

  getAllVillages(districtsId: number, search = ''): Observable<ApiResponse<VillagesResponse[]>> {
    const params: Record<string, string | number | boolean> = {
      districtsId,
    };

    if (search) params['search'] = search;

    return this.api.get<VillagesResponse[]>(DBO_URL.location.village, params);
  }
}
