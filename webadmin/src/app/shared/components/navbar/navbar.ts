import { AsyncPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthStateService } from '../../../core/services/auth-state.service';
import { MatIcon } from '@angular/material/icon';

@Component({
  selector: 'app-navbar',
  imports: [AsyncPipe, RouterLink, RouterLinkActive, MatIcon],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
})
export class NavbarComponent {
  private readonly authState = inject(AuthStateService);
  private readonly router = inject(Router);
  protected readonly menuList$ = this.authState.menuList$;

  protected async logout() {
    await this.authState.signOut();
    await this.router.navigateByUrl('/login');
  }
}
