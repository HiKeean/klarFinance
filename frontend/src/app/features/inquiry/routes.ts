import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';
import { MainLayoutComponent } from '../../core/layouts/main-layout/main-layout';

export const inquiryRoutes: Routes = [
  {
    path: '',
    canActivate: [authGuard],
    component: MainLayoutComponent,
    children: [
      { path: 'inquiry', loadComponent: () => import('./presentation/pages/inquiry-list/inquiry-list').then((m) => m.InquiryListPage) },
      { path: 'bm/inquiry', loadComponent: () => import('./presentation/pages/inquiry-list/inquiry-list').then((m) => m.InquiryListPage) }
    ]
  }
];
