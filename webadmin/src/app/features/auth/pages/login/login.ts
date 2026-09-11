import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { lastValueFrom } from 'rxjs';
import { AuthApiService } from '../../services/auth-api.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class LoginPage {
  private readonly authApi = inject(AuthApiService);
  private readonly router = inject(Router);
  protected identity = '';
  protected password = '';
  protected readonly loading = signal(false);
  protected readonly errorMessage = signal('');

  protected async submit() {
    this.errorMessage.set('');
    this.loading.set(true);
    try {
      const response = await lastValueFrom(this.authApi.login({ identity: this.identity, password: this.password }));
      if (!response.success) throw new Error(response.message);
      await this.router.navigateByUrl('/dashboard');
    } catch (err:any){
      const errorMsg = err.message || 'Identity atau password salah. Coba lagi.';
      this.errorMessage.set(errorMsg);
    } finally {
      this.loading.set(false);
    }
  }
}
