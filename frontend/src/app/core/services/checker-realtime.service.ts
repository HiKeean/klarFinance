import { Injectable, inject } from '@angular/core';
import { Client } from '@stomp/stompjs';
import { Subject } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthStateService } from './auth-state.service';

export interface AssignmentNotification {
  type: 'ASSIGNED' | 'EXPIRED';
  applicationId: number;
  message: string;
}

/**
 * Presence + assignment real-time buat Checker (konfirmasi user 2026-08-31): connect begitu buka
 * halaman Approval -> backend anggap dia "online" & bisa di-assign aplikasi baru random. Disconnect
 * TIDAK langsung lepas aplikasi yang lagi dipegang - itu diatur backend via Redis TTL 2 jam.
 */
@Injectable({ providedIn: 'root' })
export class CheckerRealtimeService {
  private readonly authState = inject(AuthStateService);
  private client: Client | null = null;
  private readonly notificationSubject = new Subject<AssignmentNotification>();
  readonly notifications$ = this.notificationSubject.asObservable();

  /**
   * Emits every time the STOMP connection is (re-)established - not just the first connect.
   * Ada race antara `markOnline()` di backend (yang langsung nyoba assign begitu server terima
   * SessionConnectedEvent) dengan client selesai `subscribe('/user/queue/assignment')` - kalau
   * assignment+push kejadian PAS di jendela itu, notifikasinya ke-drop diam-diam (Spring gak
   * retry/gak error kalau gak ada subscriber aktif). Sama juga kalau koneksi sempat putus lalu
   * auto-reconnect (`reconnectDelay`) - assignment yang kejadian pas offline juga bisa kelewat.
   * Consumer (ApprovalListPage) subscribe ini buat re-fetch antrean tiap kali kejadian, bukan
   * cuma pas dapet notifikasi WS - biar apapun yang kelewat langsung kesinkron ulang.
   */
  private readonly connectedSubject = new Subject<void>();
  readonly connected$ = this.connectedSubject.asObservable();

  async connect(): Promise<void> {
    if (this.client?.active) return;
    const token = await this.authState.getAccessToken();
    if (!token) return;

    const wsUrl = `${environment.api.baseUrl.replace(/^http/, 'ws')}/ws?token=${encodeURIComponent(token)}`;
    this.client = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 5000,
      onConnect: () => {
        this.client?.subscribe('/user/queue/assignment', (message) => {
          try {
            this.notificationSubject.next(JSON.parse(message.body));
          } catch {
            // abaikan payload yang gak valid
          }
        });
        this.connectedSubject.next();
      }
    });
    this.client.activate();
  }

  disconnect(): void {
    void this.client?.deactivate();
    this.client = null;
  }
}
