import { approvalRoutes } from './routes';

describe('approvalRoutes', () => {
  it('[positive] guards the parent route with authGuard and declares all child paths', () => {
    expect(approvalRoutes[0].canActivate).toBeTruthy();
    const childPaths = (approvalRoutes[0].children ?? []).map((c) => c.path);
    expect(childPaths).toEqual(['approval', 'approval/:id', 'bm/approval', 'bm/approval/:id', 'bm/loan-review/:id']);
  });

  it('[positive] every child route lazily resolves to a defined component', async () => {
    for (const child of approvalRoutes[0].children ?? []) {
      const loadComponent = (child as any).loadComponent;
      const resolved = await loadComponent();
      expect(resolved).toBeTruthy();
    }
  });
});
