import { EnvironmentProviders } from '@angular/core';
import { provideFirebaseApp } from '@angular/fire/app';
import { provideAuth } from '@angular/fire/auth';
import { provideFirestore } from '@angular/fire/firestore';
import { provideStorage } from '@angular/fire/storage';
import { environment } from '../environments/environment';
import { initializeApp } from 'firebase/app';
import { connectAuthEmulator, getAuth } from 'firebase/auth';
import { connectFirestoreEmulator, getFirestore } from 'firebase/firestore';
import { connectStorageEmulator, getStorage } from 'firebase/storage';

const isBrowser = typeof window !== 'undefined';

export const firebaseProviders: EnvironmentProviders[] = [
  provideFirebaseApp(() => initializeApp(environment.firebase)),
  ...(isBrowser
    ? [
        provideAuth(() => {
          const auth = getAuth();

          if (environment.useEmulators) {
            connectAuthEmulator(auth, environment.emulators.auth, {
              disableWarnings: true
            });
          }

          return auth;
        }),
        provideFirestore(() => {
          const firestore = getFirestore();

          if (environment.useEmulators) {
            connectFirestoreEmulator(
              firestore,
              environment.emulators.firestore.host,
              environment.emulators.firestore.port
            );
          }

          return firestore;
        }),
        provideStorage(() => {
          const storage = getStorage();

          if (environment.useEmulators) {
            connectStorageEmulator(
              storage,
              environment.emulators.storage.host,
              environment.emulators.storage.port
            );
          }

          return storage;
        })
      ]
    : [])
];
