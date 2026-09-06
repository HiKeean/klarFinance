import { AsyncPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { AuthStateService } from '../../../core/services/auth-state.service';

@Component({
  selector: 'app-header',
  imports: [AsyncPipe, MatIconModule, RouterLink],
  templateUrl: './header.html',
  styleUrl: './header.css'
})
export class HeaderComponent {
  private readonly authState = inject(AuthStateService);
  private readonly router = inject(Router);
  protected readonly userProfile$ = this.authState.userProfile$;

  protected async logout() {
    await this.authState.signOut();
    await this.router.navigateByUrl('/login');
  }
}
