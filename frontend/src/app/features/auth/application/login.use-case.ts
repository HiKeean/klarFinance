import { Injectable, inject } from '@angular/core';
import { Observable, switchMap } from 'rxjs';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { AuthRepository } from '../domain/repositories/auth.repository';
import { AuthSession, LoginCredentials } from '../domain/entities/auth-session';
import { isRoleAllowed } from '../domain/policies/allowed-roles.policy';

@Injectable({ providedIn: 'root' })
export class LoginUseCase {
  private readonly authRepository = inject(AuthRepository);
  private readonly authState = inject(AuthStateService);

  execute(credentials: LoginCredentials): Observable<AuthSession> {
    return this.authRepository.login(credentials).pipe(
      switchMap(async (session) => {
        if (!isRoleAllowed(session.userProfile.role)) {
          throw new Error('Akun ini tidak memiliki akses ke aplikasi Checker & BM.');
        }
        await this.authState.saveSession(session.accessToken, session.refreshToken, session.userProfile, session.menu);
        return session;
      })
    );
  }
}
