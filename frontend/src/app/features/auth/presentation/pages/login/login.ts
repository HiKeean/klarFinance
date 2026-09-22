import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { lastValueFrom } from 'rxjs';
import { LoginUseCase } from '../../../application/login.use-case';
import { PasswordResetApiService } from '../../../infrastructure/password-reset-api.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class LoginPage {
  private readonly loginUseCase = inject(LoginUseCase);
  private readonly passwordResetApi = inject(PasswordResetApiService);
  private readonly router = inject(Router);
  protected identity = '';
  protected password = '';
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal('');

  protected readonly showResetForm = signal(false);
  protected resetIdentity = '';
  protected readonly resetSubmitting = signal(false);
  protected readonly resetSuccessMessage = signal('');
  protected readonly resetErrorMessage = signal('');

  protected async submit() {
    this.errorMessage.set('');
    this.loading.set(true);
    try {
      await lastValueFrom(this.loginUseCase.execute({ identity: this.identity, password: this.password }));
      await this.router.navigateByUrl('/dashboard');
    } catch (err: any) {
      const errorMsg = err.message || 'Identity atau password salah. Coba lagi.';
      this.errorMessage.set(errorMsg);
    } finally {
      this.loading.set(false);
    }
  }

  protected onIdentityChange(value: string) {
    this.identity = value;
    this.showResetForm.set(false);
  }

  protected openResetForm() {
    this.resetIdentity = this.identity;
    this.resetSuccessMessage.set('');
    this.resetErrorMessage.set('');
    this.showResetForm.set(true);
  }

  protected closeResetForm() {
    this.showResetForm.set(false);
  }

  protected async submitResetRequest() {
    this.resetErrorMessage.set('');
    this.resetSuccessMessage.set('');
    this.resetSubmitting.set(true);
    try {
      await lastValueFrom(this.passwordResetApi.submit(this.resetIdentity));
      this.resetSuccessMessage.set(
        'Permintaan reset password terkirim. Admin akan memproses dan mengirimkan password baru lewat WhatsApp.'
      );
    } catch (err: any) {
      this.resetErrorMessage.set(err.message || 'Gagal mengirim permintaan reset password. Coba lagi.');
    } finally {
      this.resetSubmitting.set(false);
    }
  }
}
