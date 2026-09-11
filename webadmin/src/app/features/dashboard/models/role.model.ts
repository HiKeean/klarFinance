export interface Role {
  id: number;
  role: string | null;
}
export interface Menu {
  id: number;
  url: string | null;
  name: string | null;
  logo: string | null;
  createdBy: string | null;
  createdAt: string | null;
}
/** Mirrors backend `GetAllRoleMenu` DTO — one row per role<->menu assignment. */
export interface RoleMenu {
  id: number;
  roleId: number;
  role: string | null;
  menuId: number;
  menu: string | null;
  url: string | null;
  logo: string | null;
  assignedBy: string | null;
  assignedAt: string | null;
}

