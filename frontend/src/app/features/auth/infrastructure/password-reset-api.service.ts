import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { AUTH_URL } from '../../../core/config/url';

/** Dipakai dari halaman login - tombol "Lupa Password" (muncul setelah 3x salah password)
 * submit identity ke sini, masuk antrean di webadmin buat di-approve/reject Superadmin. */
@Injectable({ providedIn: 'root' })
export class PasswordResetApiService {
  private readonly api = inject(ApiService);

  submit(identity: string): Observable<void> {
    return this.api.post<unknown>(AUTH_URL.passwordResetRequest, { identity }).pipe(
      map((response) => {
        if (!response.success) throw new Error(response.message);
      })
    );
  }
}
