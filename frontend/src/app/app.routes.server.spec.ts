import { RenderMode } from '@angular/ssr';
import { serverRoutes } from './app.routes.server';

describe('serverRoutes', () => {
  it('[positive] every declared client-facing path has a renderMode', () => {
    const paths = ['login', 'reauth', 'dashboard', 'approval', 'approval/:id', 'bm/approval', 'bm/approval/:id', 'bm/loan-review/:id', 'inquiry', 'bm/inquiry'];
    for (const path of paths) {
      const route = serverRoutes.find((r) => r.path === path);
      expect(route?.renderMode).toBe(RenderMode.Client);
    }
  });

  it('[positive] the catch-all route prerenders', () => {
    const wildcard = serverRoutes.find((r) => r.path === '**');
    expect(wildcard?.renderMode).toBe(RenderMode.Prerender);
  });
});
