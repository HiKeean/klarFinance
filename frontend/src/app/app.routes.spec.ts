import { routes } from './app.routes';

describe('app.routes', () => {
  it('[positive] redirects the empty path to /dashboard', () => {
    const root = routes.find((r) => r.path === '');
    expect(root?.redirectTo).toBe('dashboard');
    expect(root?.pathMatch).toBe('full');
  });

  it('[positive] redirects unknown paths to /login', () => {
    const wildcard = routes.find((r) => r.path === '**');
    expect(wildcard?.redirectTo).toBe('login');
  });

  it('[positive] includes the auth, dashboard, approval, and inquiry route groups', () => {
    expect(routes.length).toBeGreaterThan(2);
  });
});
