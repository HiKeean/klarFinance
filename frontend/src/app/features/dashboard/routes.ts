import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';
import { MainLayoutComponent } from '../../core/layouts/main-layout/main-layout';

export const dashboardRoutes: Routes = [
  {
    path: '',
    canActivate: [authGuard],
    component: MainLayoutComponent,
    children: [
      { path: 'dashboard', loadComponent: () => import('./presentation/pages/dashboard/dashboard').then((m) => m.DashboardPage) }
    ]
  }
];
