import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';
import { MainLayoutComponent } from '../../core/layouts/main-layout/main-layout';

export const approvalRoutes: Routes = [
  {
    path: '',
    canActivate: [authGuard],
    component: MainLayoutComponent,
    children: [
      { path: 'approval', loadComponent: () => import('./presentation/pages/approval-list/approval-list').then((m) => m.ApprovalListPage) },
      { path: 'approval/:id', loadComponent: () => import('./presentation/pages/approval-detail/approval-detail').then((m) => m.ApprovalDetailPage) },
      { path: 'bm/approval', loadComponent: () => import('./presentation/pages/approval-list/approval-list').then((m) => m.ApprovalListPage) },
      { path: 'bm/approval/:id', loadComponent: () => import('./presentation/pages/approval-detail/approval-detail').then((m) => m.ApprovalDetailPage) },
      { path: 'bm/loan-review/:id', loadComponent: () => import('./presentation/pages/loan-review-detail/loan-review-detail').then((m) => m.LoanReviewDetailPage) }
    ]
  }
];
