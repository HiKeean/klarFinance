export interface BranchListItem {
  branchCode: number;
  name: string;
  address: string | null;
  villageName: string | null;
  bmIdentity: string | null;
  bmName: string | null;
}

/** BM user, dipetakan dari GET /admin/auth/users?role=BM - branchCode null berarti BM ini belum
 * di-assign ke branch manapun. */
export interface BmOption {
  identity: string;
  name: string | null;
  branchCode: number | null;
  branchName: string | null;
}
