import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { SUPERADMIN_URL } from '../../../core/config/url';
import { ApiResponse } from '../../../shared/models/api-response';
import { NplReportItem } from '../models/npl-report.model';

@Injectable({ providedIn: 'root' })
export class NplReportServices {
  private readonly api = inject(ApiService);

  getReport(): Observable<ApiResponse<NplReportItem[]>> {
    return this.api.get<NplReportItem[]>(SUPERADMIN_URL.dbo.nplReport);
  }
}
