import { TestBed } from '@angular/core/testing';
import { CheckerRealtimeService } from './checker-realtime.service';
import { AuthStateService } from './auth-state.service';

const activateMock = vi.fn();
const deactivateMock = vi.fn();
const subscribeMock = vi.fn();
let lastClientOptions: any;

vi.mock('@stomp/stompjs', () => {
  class FakeClient {
    active = false;
    constructor(options: any) {
      lastClientOptions = options;
    }
    activate() {
      activateMock();
      this.active = true;
    }
    deactivate() {
      deactivateMock();
      this.active = false;
      return Promise.resolve();
    }
    subscribe(...args: unknown[]) {
      return subscribeMock(...args);
    }
  }
  return { Client: FakeClient };
});

describe('CheckerRealtimeService', () => {
  let authState: { getAccessToken: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    activateMock.mockClear();
    deactivateMock.mockClear();
    subscribeMock.mockClear();
    lastClientOptions = undefined;
    authState = { getAccessToken: vi.fn() };
    TestBed.configureTestingModule({
      providers: [CheckerRealtimeService, { provide: AuthStateService, useValue: authState }]
    });
  });

  it('[negative] connect() does nothing when there is no access token', async () => {
    authState.getAccessToken.mockResolvedValue(null);
    const service = TestBed.inject(CheckerRealtimeService);
    await service.connect();
    expect(activateMock).not.toHaveBeenCalled();
  });

  it('[positive] connect() builds a Client with the token in the ws URL and activates it', async () => {
    authState.getAccessToken.mockResolvedValue('my-token');
    const service = TestBed.inject(CheckerRealtimeService);
    await service.connect();
    expect(lastClientOptions.brokerURL).toContain('token=my-token');
    expect(activateMock).toHaveBeenCalled();
  });

  it('[positive] onConnect subscribes to the assignment queue and pushes a valid notification', async () => {
    authState.getAccessToken.mockResolvedValue('my-token');
    const service = TestBed.inject(CheckerRealtimeService);
    await service.connect();

    let received: unknown;
    service.notifications$.subscribe((n) => (received = n));

    lastClientOptions.onConnect();
    expect(subscribeMock).toHaveBeenCalledWith('/user/queue/assignment', expect.any(Function));

    const messageHandler = subscribeMock.mock.calls[0][1];
    messageHandler({ body: JSON.stringify({ type: 'ASSIGNED', applicationId: 1, message: 'hi' }) });
    expect(received).toEqual({ type: 'ASSIGNED', applicationId: 1, message: 'hi' });
  });

  it('[negative] onConnect silently ignores a malformed message payload', async () => {
    authState.getAccessToken.mockResolvedValue('my-token');
    const service = TestBed.inject(CheckerRealtimeService);
    await service.connect();

    let received: unknown;
    let errored = false;
    service.notifications$.subscribe({ next: (n) => (received = n), error: () => (errored = true) });

    lastClientOptions.onConnect();
    const messageHandler = subscribeMock.mock.calls[0][1];
    expect(() => messageHandler({ body: 'not-json' })).not.toThrow();
    expect(received).toBeUndefined();
    expect(errored).toBe(false);
  });

  it('[positive] onConnect emits on connected$ every time the connection is (re-)established', async () => {
    authState.getAccessToken.mockResolvedValue('my-token');
    const service = TestBed.inject(CheckerRealtimeService);
    await service.connect();

    let count = 0;
    service.connected$.subscribe(() => count++);
    lastClientOptions.onConnect();
    lastClientOptions.onConnect();
    expect(count).toBe(2);
  });

  it('[negative] connect() is a no-op when the client is already active', async () => {
    authState.getAccessToken.mockResolvedValue('my-token');
    const service = TestBed.inject(CheckerRealtimeService);
    await service.connect();
    activateMock.mockClear();
    authState.getAccessToken.mockClear();

    await service.connect();
    expect(authState.getAccessToken).not.toHaveBeenCalled();
    expect(activateMock).not.toHaveBeenCalled();
  });

  it('[positive] disconnect() deactivates the client and nulls it out (idempotent)', async () => {
    authState.getAccessToken.mockResolvedValue('my-token');
    const service = TestBed.inject(CheckerRealtimeService);
    await service.connect();

    service.disconnect();
    expect(deactivateMock).toHaveBeenCalledTimes(1);

    service.disconnect();
    expect(deactivateMock).toHaveBeenCalledTimes(1);
  });
});
