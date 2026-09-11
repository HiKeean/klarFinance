export interface User {
  identity: string | null;
  name: string;
  role: string | null;
  noHp: string;
  branch: {
    branchCode: number;
    name: string;
  };
  createdAt: string;
  updatedAt: string;
  deletedAt: string | null;
}

export interface UserPageResponse {
  content: User[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
}

