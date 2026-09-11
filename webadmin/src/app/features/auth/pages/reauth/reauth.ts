import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { lastValueFrom } from 'rxjs';
import { AuthApiService } from '../../services/auth-api.service';
import { AuthStateService } from '../../../../core/services/auth-state.service';

@Component({ selector: 'app-reauth', imports: [FormsModule], templateUrl: './reauth.html', styleUrl: '../login/login.css' })
export class ReauthPage {
  private readonly authApi = inject(AuthApiService);
  private readonly authState = inject(AuthStateService);
  private readonly router = inject(Router);
  protected identity = '';
  protected password = '';
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal('');

  constructor() { void this.loadIdentity(); }

  private async loadIdentity() { this.identity = (await this.authState.getIdentity()) || ''; }

  protected async submit() {
    this.errorMessage.set('');
    this.loading.set(true);
    try {
      const response = await lastValueFrom(this.authApi.login({ identity: this.identity, password: this.password }));
      if (!response.success) throw new Error(response.message);
      await this.router.navigateByUrl('/dashboard');
    } catch (err: any) {
      this.errorMessage.set(err.message || 'Password salah. Coba lagi.');
    } finally { this.loading.set(false); }
  }

  protected async changeAccount() {
    await this.authState.signOut();
    await this.router.navigateByUrl('/login');
  }
}
