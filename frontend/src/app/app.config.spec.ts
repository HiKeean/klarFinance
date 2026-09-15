import { TestBed } from '@angular/core/testing';
import { appConfig } from './app.config';
import { AuthRepository } from './features/auth/domain/repositories/auth.repository';
import { AuthApiRepository } from './features/auth/infrastructure/auth-api.repository';

describe('appConfig', () => {
  it('[positive] declares a non-empty provider set', () => {
    expect(appConfig.providers.length).toBeGreaterThan(0);
  });

  it('[positive] wires AuthRepository to AuthApiRepository', () => {
    TestBed.configureTestingModule(appConfig);
    const repository = TestBed.inject(AuthRepository);
    expect(repository).toBeInstanceOf(AuthApiRepository);
  });
});
