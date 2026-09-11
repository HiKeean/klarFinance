import {Routes} from '@angular/router';

export const landingPageRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('./landingpage/landingpage').then((m) => m.Landingpage)
  }
]
