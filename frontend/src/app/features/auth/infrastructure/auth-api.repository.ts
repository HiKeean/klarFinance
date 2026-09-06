import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { INTERNAL_URL } from '../../../core/config/url';
import { MenuItem, UserProfile } from '../../../core/services/auth-state.service';
import { AuthRepository } from '../domain/repositories/auth.repository';
import { AuthSession, LoginCredentials } from '../domain/entities/auth-session';

interface LoginResponseDto {
  accessToken?: string;
  refreshToken?: string;
  userProfile?: UserProfile;
  menu?: MenuItem[];
}

@Injectable({ providedIn: 'root' })
export class AuthApiRepository implements AuthRepository {
  private readonly api = inject(ApiService);

  login(credentials: LoginCredentials): Observable<AuthSession> {
    return this.api.post<LoginResponseDto>(INTERNAL_URL.auth.login, credentials).pipe(
      map((response) => {
        if (!response.success) throw new Error(response.message);
        const data = response.data;
        if (!data?.accessToken || !data?.refreshToken) throw new Error('Server Error');

        return {
          accessToken: data.accessToken,
          refreshToken: data.refreshToken,
          userProfile: data.userProfile ?? { identity: credentials.identity, name: 'User' },
          menu: data.menu ?? []
        };
      })
    );
  }
}
