import { Routes } from '@angular/router';
import { loginGuard } from '../../core/guards/auth.guard';

export const authRoutes: Routes = [
  { path: 'login', canActivate: [loginGuard], loadComponent: () => import('./pages/login/login').then((m) => m.LoginPage) },
  { path: 'reauth', loadComponent: () => import('./pages/reauth/reauth').then((m) => m.ReauthPage) }
];
