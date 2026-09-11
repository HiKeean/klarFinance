export interface LoginResponse {
  accessToken?: string;
  refreshToken?: string;
  userProfile?: UserProfile;
  menu?: Menu[];
}

export interface UserProfile {
  identity: string;
  name: string | null;
  role?: string;
}

export interface Menu {
  url: string;
  name: string;
  logo: string;
}
