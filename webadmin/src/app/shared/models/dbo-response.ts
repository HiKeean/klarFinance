export interface ProvinceResponse {
  id: number;
  name: string;
}

export interface BranchResponse {
  id?: number;
  branchCode: number;
  name: string;
}

export interface RegenciesResponse {
  id: number;
  name: string;
  province: ProvinceResponse;
}

export interface DistrictsResponse {
  id: number;
  name: string;
  regency: RegenciesResponse;
}

export interface VillagesResponse {
  id: number;
  name: string;
  district: DistrictsResponse | null;
}
