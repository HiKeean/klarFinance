import { environment } from '../../../environments/environment';

const BASE_API = `${environment.api.baseUrl}/`;
const BASE_INTERNAL = `${environment.api.baseUrl}/internal/`;
const BASE_SUPERADMIN = `${environment.api.baseUrl}/admin/`;

export const INTERNAL_URL = {
  auth: {
    login: `${BASE_INTERNAL}auth/login`,
  },
} as const;

export const DBO_URL = {
  // BranchController itu @AdminAnnotation ("/branch"), BUKAN di bawah prefix /dbo - path
  // sebenarnya /api/v1/admin/branch. Dulu salah nunjuk ke /api/v1/dbo/branch (404 diam-diam,
  // ketauan pas ngerjain halaman Branch baru) - dibenerin di sini biar branch-territory (yang
  // udah pakai key ini) ikut kebenerin juga.
  branch: `${BASE_SUPERADMIN}branch`,
  location:{
    province: `${BASE_API}dbo/location/provinces`,
    regency: `${BASE_API}dbo/location/regencies`,
    district: `${BASE_API}dbo/location/districts`,
    village: `${BASE_API}dbo/location/villages`,
  }
}

export const SUPERADMIN_URL = {
  auth: {
    getAllUsers: `${BASE_SUPERADMIN}auth/users`,
    assignBranch: (identity: string) => `${BASE_SUPERADMIN}auth/users/${identity}/branch`,
    registration: `${BASE_INTERNAL}auth/register`,
    deleteRegistration: `${BASE_SUPERADMIN}auth/`,
    getAllRoles: `${BASE_SUPERADMIN}auth/roles`,
    updateRole: (id: number) => `${BASE_SUPERADMIN}auth/roles/${id}`,
    deleteRole: (id: number) => `${BASE_SUPERADMIN}auth/roles/${id}`,
    saveRole: `${BASE_SUPERADMIN}auth/role`,
    getAllMenus: `${BASE_SUPERADMIN}auth/menus`,
    saveMenu: `${BASE_SUPERADMIN}auth/menu`,
    updateMenu: (id: number) => `${BASE_SUPERADMIN}auth/menus/${id}`,
    deleteMenu: (id: number) => `${BASE_SUPERADMIN}auth/menus/${id}`,
    getAllRoleMenus: `${BASE_SUPERADMIN}auth/role-menus`,
    addRoleMenu: `${BASE_SUPERADMIN}auth/role-menu`,
    updateRoleMenu: (id: number) => `${BASE_SUPERADMIN}auth/role-menus/${id}`,
    deleteRoleMenu: (id: number) => `${BASE_SUPERADMIN}auth/role-menus/${id}`,
    passwordResetRequests: `${BASE_SUPERADMIN}auth/password-reset-requests`,
    passwordResetRequestDecision: (id: number) => `${BASE_SUPERADMIN}auth/password-reset-requests/${id}/decision`,
  },
  dbo: {
    branchTerritory: `${BASE_SUPERADMIN}dbo/branch-territory`,
    branchTerritoryGaps: `${BASE_SUPERADMIN}dbo/branch-territory/gaps`,
    assignRegencyTerritory: `${BASE_SUPERADMIN}dbo/branch-territory/regency`,
    assignProvinceTerritory: `${BASE_SUPERADMIN}dbo/branch-territory/province`,
    deleteBranchTerritory: (id: number) => `${BASE_SUPERADMIN}dbo/branch-territory/${id}`,
    nplReport: `${BASE_SUPERADMIN}dbo/npl-report`,
    nplReportBranchLoans: (branchId: number) => `${BASE_SUPERADMIN}dbo/npl-report/${branchId}/loans`,
  },
};


