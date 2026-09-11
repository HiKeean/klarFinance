export const environment = {
  production: true,
  useEmulators: false,
  firebaseConfigured: false,
  api: {
    baseUrl: 'http://api.klarfinance.hizkialb.xyz/api/v1',
    // baseUrl: 'https://your-api-domain.com/api',
    apiKey: '8815d46823660294132c4b785b4b2cccd7b3d7e9',
    secretKey: '404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970'
  },
  firebase: {
    apiKey: 'your-api-key',
    authDomain: 'your-project-id.firebaseapp.com',
    projectId: 'your-project-id',
    storageBucket: 'your-project-id.firebasestorage.app',
    messagingSenderId: 'your-messaging-sender-id',
    appId: 'your-app-id',
    measurementId: 'your-measurement-id'
  },
  emulators: {
    auth: 'http://127.0.0.1:9099',
    firestore: {
      host: '127.0.0.1',
      port: 8080
    },
    storage: {
      host: '127.0.0.1',
      port: 9199
    }
  }
} as const;
