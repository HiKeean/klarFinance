import { Route } from '@angular/router';
import { dashboardRoutes } from './routes';

/**
 * dashboardRoutes' children are only reached (and their loadComponent closures only executed)
 * when the router actually navigates to each path. app.routes.spec.ts only navigates to the
 * dashboard root ('') via the guard-redirect integration tests, so the other child routes'
 * lazy imports are otherwise never exercised. Invoke each loadComponent directly - cheap and
 * exercises the exact same dynamic import the router would trigger, without needing a full
 * router harness or backend mocking for every page.
 */
describe('dashboardRoutes', () => {
  const children = (dashboardRoutes[0].children ?? []) as Route[];

  it('[positive] defines the expected child paths', () => {
    expect(children.map((route) => route.path)).toEqual([
      '', 'user', 'role', 'role/:id/menus', 'menu', 'branch', 'branch-territory',
      'npl-report', 'npl-report/:branchId', 'password-reset-requests'
    ]);
  });

  // Each loadComponent closure already unwraps the dynamic import to the component
  // class itself (`() => import(...).then((m) => m.X)`), so the resolved value IS the
  // class - not a module namespace object.

  it('[positive] "" lazy-loads DashboardPage', async () => {
    const mod = await children[0].loadComponent!();
    expect(mod).toBeTruthy();
  });

  it('[positive] "user" lazy-loads User', async () => {
    const mod = await children[1].loadComponent!();
    expect(mod).toBeTruthy();
  });

  it('[positive] "role" lazy-loads RolePage', async () => {
    const mod = await children[2].loadComponent!();
    expect(mod).toBeTruthy();
  });

  it('[positive] "role/:id/menus" lazy-loads RoleMenuPage', async () => {
    const mod = await children[3].loadComponent!();
    expect(mod).toBeTruthy();
  });

  it('[positive] "menu" lazy-loads MenuPage', async () => {
    const mod = await children[4].loadComponent!();
    expect(mod).toBeTruthy();
  });

  it('[positive] "branch" lazy-loads BranchPage', async () => {
    const mod = await children[5].loadComponent!();
    expect(mod).toBeTruthy();
  });

  it('[positive] "branch-territory" lazy-loads BranchTerritoryPage', async () => {
    const mod = await children[6].loadComponent!();
    expect(mod).toBeTruthy();
  });

  it('[positive] "npl-report" lazy-loads NplReportPage', async () => {
    const mod = await children[7].loadComponent!();
    expect(mod).toBeTruthy();
  });

  it('[positive] "npl-report/:branchId" lazy-loads NplBranchDetailPage', async () => {
    const mod = await children[8].loadComponent!();
    expect(mod).toBeTruthy();
  });

  it('[positive] "password-reset-requests" lazy-loads PasswordResetRequestsPage', async () => {
    const mod = await children[9].loadComponent!();
    expect(mod).toBeTruthy();
  });
});
