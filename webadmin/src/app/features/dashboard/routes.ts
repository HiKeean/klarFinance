import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';
import { AdminLayoutComponent } from '../../core/layouts/admin-layout/admin-layout';

export const dashboardRoutes: Routes = [
  {
    path: 'dashboard',
    component: AdminLayoutComponent,
    canActivateChild: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/dashboard/dashboard').then((m) => m.DashboardPage)
      },
      {
        path: 'user',
        loadComponent: () => import('./pages/user/user').then((m) => m.User)
      },
      {
        path: 'role',
        loadComponent: () => import('./pages/role/role').then((m) => m.RolePage)
      },
      {
        path: 'role/:id/menus',
        loadComponent: () => import('./pages/role-menu/role-menu').then((m) => m.RoleMenuPage)
      },
      {
        path: 'menu',
        loadComponent: () => import('./pages/menu/menu').then((m) => m.MenuPage)
      },
      {
        path: 'branch',
        loadComponent: () => import('./pages/branch/branch').then((m) => m.BranchPage)
      },
      {
        path: 'branch-territory',
        loadComponent: () => import('./pages/branch-territory/branch-territory').then((m) => m.BranchTerritoryPage)
      },
      {
        path: 'npl-report',
        loadComponent: () => import('./pages/npl-report/npl-report').then((m) => m.NplReportPage)
      },
      {
        path: 'npl-report/:branchId',
        loadComponent: () => import('./pages/npl-branch-detail/npl-branch-detail').then((m) => m.NplBranchDetailPage)
      },
      {
        path: 'password-reset-requests',
        loadComponent: () => import('./pages/password-reset-requests/password-reset-requests').then((m) => m.PasswordResetRequestsPage)
      },
    ]
  }
];
