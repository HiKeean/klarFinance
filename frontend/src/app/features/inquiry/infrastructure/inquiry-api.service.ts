import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { INTERNAL_URL } from '../../../core/config/url';
import { InquiryResult } from '../domain/entities/inquiry-result';

@Injectable({ providedIn: 'root' })
export class InquiryApiService {
  private readonly api = inject(ApiService);

  search(q: string): Observable<InquiryResult[]> {
    return this.api.get<InquiryResult[]>(INTERNAL_URL.los.limitApplicationsSearch, { q }).pipe(
      map((response) => {
        if (!response.success) throw new Error(response.message);
        return response.data;
      })
    );
  }
}
