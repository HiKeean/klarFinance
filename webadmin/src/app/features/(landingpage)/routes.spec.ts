import { landingPageRoutes } from './routes';

describe('landingPageRoutes', () => {
  it('[positive] "" lazy-loads Landingpage', async () => {
    const mod = await landingPageRoutes[0].loadComponent!();
    expect(mod).toBeTruthy();
  });
});
