import { config } from './app.config.server';

describe('app.config.server', () => {
  it('[positive] merges server rendering providers into the base app config', () => {
    expect(config.providers.length).toBeGreaterThan(0);
  });
});
