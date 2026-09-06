import { Routes } from '@angular/router';
import { authRoutes } from './features/auth/routes';
import { dashboardRoutes } from './features/dashboard/routes';
import { approvalRoutes } from './features/approval/routes';
import { inquiryRoutes } from './features/inquiry/routes';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  ...authRoutes,
  ...dashboardRoutes,
  ...approvalRoutes,
  ...inquiryRoutes,
  { path: '**', redirectTo: 'login' }
];
