import { Injectable, inject } from '@angular/core';
import { Observable, switchMap } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { INTERNAL_URL } from '../../../core/config/url';
import { ApiResponse } from '../../../shared/models/api-response';
import { LoginRequest } from '../models/login-request';
import { LoginResponse } from '../models/login-response';
import { AuthStateService } from '../../../core/services/auth-state.service';

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly api = inject(ApiService);
  private readonly authState = inject(AuthStateService);

  login(payload: LoginRequest): Observable<ApiResponse<LoginResponse>> {
    return this.api.post<LoginResponse>(INTERNAL_URL.auth.login, payload).pipe(
      switchMap(async (response) => {

        if (response.success && response.data) {
          if (response.data.accessToken === undefined || response.data.refreshToken === undefined) {
            throw new Error('Server Error');
          }

          const accessToken = response.data.accessToken;
          const refreshToken = response.data.refreshToken;

          const userProfile = response.data.userProfile || {
            identity: payload.identity,
            name: "User"
          };
          const menuList = response.data.menu || [];
          await this.authState.saveSession(accessToken, refreshToken, userProfile, menuList);
        }
        return response;
      })
    );
  }

}
