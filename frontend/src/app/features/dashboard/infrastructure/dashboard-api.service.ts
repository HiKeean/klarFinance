import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { INTERNAL_URL } from '../../../core/config/url';
import { DashboardSummary } from '../domain/entities/dashboard-summary';

@Injectable({ providedIn: 'root' })
export class DashboardApiService {
  private readonly api = inject(ApiService);

  getSummary(): Observable<DashboardSummary> {
    return this.api.get<DashboardSummary>(INTERNAL_URL.los.dashboardSummary).pipe(
      map((response) => {
        if (!response.success) throw new Error(response.message);
        return response.data;
      })
    );
  }
}
