import { inquiryRoutes } from './routes';

describe('inquiryRoutes', () => {
  it('[positive] guards the parent route and declares both inquiry child paths', () => {
    expect(inquiryRoutes[0].canActivate).toBeTruthy();
    const childPaths = (inquiryRoutes[0].children ?? []).map((c) => c.path);
    expect(childPaths).toEqual(['inquiry', 'bm/inquiry']);
  });

  it('[positive] every child route lazily resolves to a defined component', async () => {
    for (const child of inquiryRoutes[0].children ?? []) {
      const loadComponent = (child as any).loadComponent;
      const resolved = await loadComponent();
      expect(resolved).toBeTruthy();
    }
  });
});
