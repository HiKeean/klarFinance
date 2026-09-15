import { dashboardRoutes } from './routes';

describe('dashboardRoutes', () => {
  it('[positive] guards the parent route and declares the /dashboard child path', () => {
    expect(dashboardRoutes[0].canActivate).toBeTruthy();
    const childPaths = (dashboardRoutes[0].children ?? []).map((c) => c.path);
    expect(childPaths).toEqual(['dashboard']);
  });

  it('[positive] the dashboard child route lazily resolves to a defined component', async () => {
    const loadComponent = (dashboardRoutes[0].children?.[0] as any).loadComponent;
    const resolved = await loadComponent();
    expect(resolved).toBeTruthy();
  });
});
