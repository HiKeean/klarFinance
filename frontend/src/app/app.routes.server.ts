import { RenderMode, ServerRoute } from '@angular/ssr';

export const serverRoutes: ServerRoute[] = [
  { path: 'login', renderMode: RenderMode.Client },
  { path: 'reauth', renderMode: RenderMode.Client },
  { path: 'dashboard', renderMode: RenderMode.Client },
  { path: 'approval', renderMode: RenderMode.Client },
  { path: 'approval/:id', renderMode: RenderMode.Client },
  { path: 'bm/approval', renderMode: RenderMode.Client },
  { path: 'bm/approval/:id', renderMode: RenderMode.Client },
  { path: 'bm/loan-review/:id', renderMode: RenderMode.Client },
  { path: 'inquiry', renderMode: RenderMode.Client },
  { path: 'bm/inquiry', renderMode: RenderMode.Client },
  {
    path: '**',
    renderMode: RenderMode.Prerender
  }
];
