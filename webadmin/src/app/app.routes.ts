import { Routes } from '@angular/router';
import { authRoutes } from './features/auth/routes';
import { dashboardRoutes } from './features/dashboard/routes';
import {landingPageRoutes} from './features/(landingpage)';

export const routes: Routes = [
  ...landingPageRoutes,
  ...authRoutes,
  ...dashboardRoutes,
  { path: '**', redirectTo: 'login' }
];
