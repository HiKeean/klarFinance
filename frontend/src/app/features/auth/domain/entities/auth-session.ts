import { MenuItem, UserProfile } from '../../../../core/services/auth-state.service';

export interface LoginCredentials {
  identity: string;
  password: string;
}

export interface AuthSession {
  accessToken: string;
  refreshToken: string;
  userProfile: UserProfile;
  menu: MenuItem[];
}
