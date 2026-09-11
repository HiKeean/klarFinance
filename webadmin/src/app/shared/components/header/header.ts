import { AsyncPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { combineLatest } from 'rxjs';
import { filter, map, startWith } from 'rxjs/operators';
import { AuthStateService, MenuItem } from '../../../core/services/auth-state.service';

@Component({
  selector: 'app-header',
  imports: [AsyncPipe],
  templateUrl: './header.html',
  styleUrl: './header.css'
})
export class HeaderComponent {
  private readonly authState = inject(AuthStateService);
  private readonly router = inject(Router);
  protected readonly userProfile$ = this.authState.userProfile$;

  private readonly currentUrl$ = this.router.events.pipe(
    filter((event): event is NavigationEnd => event instanceof NavigationEnd),
    map((event) => event.urlAfterRedirects),
    startWith(this.router.url),
  );

  protected readonly pageTitle = toSignal(
    combineLatest([this.currentUrl$, this.authState.menuList$]).pipe(
      map(([url, menuList]) => this.getPageTitle(url, menuList)),
    ),
    { initialValue: 'Dashboard' },
  );

  /** Header title mirrors the RBAC menu name for the active route; falls back to "Dashboard" when no menu matches (e.g. the dashboard home). */
  private getPageTitle(url: string, menuList: MenuItem[]): string {
    const path = url.split('?')[0];
    return menuList.find((item) => item.url === path)?.name ?? 'Dashboard';
  }
}
