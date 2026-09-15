import { authRoutes } from './routes';

describe('authRoutes', () => {
  it('[positive] guards /login with loginGuard', () => {
    const login = authRoutes.find((r) => r.path === 'login');
    expect(login?.canActivate).toBeTruthy();
  });

  it('[positive] declares /reauth without a guard', () => {
    const reauth = authRoutes.find((r) => r.path === 'reauth');
    expect(reauth).toBeTruthy();
    expect(reauth?.canActivate).toBeUndefined();
  });

  it('[positive] every route lazily resolves to a defined component', async () => {
    for (const route of authRoutes) {
      const loadComponent = (route as any).loadComponent;
      const resolved = await loadComponent();
      expect(resolved).toBeTruthy();
    }
  });
});
