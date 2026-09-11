export interface BranchTerritory {
  id: number;
  branchId: number;
  branchName: string | null;
  regencyId: number;
  regencyName: string | null;
  provinceName: string | null;
}

export interface RegencyGap {
  regencyId: number;
  regencyName: string | null;
  provinceName: string | null;
}

export interface BranchOption {
  id: number;
  name: string;
}
