import { AsyncPipe } from '@angular/common';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { AuthStateService } from '../../../../../core/services/auth-state.service';
import { DashboardApiService } from '../../../infrastructure/dashboard-api.service';
import { DashboardSummary, LoanStatusSlice } from '../../../domain/entities/dashboard-summary';

const SLICE_COLOR_BY_LABEL: Record<string, string> = {
  Current: 'var(--slice-current)',
  Overdue: 'var(--slice-overdue)'
};
const FALLBACK_SLICE_COLOR = 'var(--slice-fallback)';

const NPL_SEVERITY_LABEL: Record<string, string> = {
  GREEN: 'Sehat',
  YELLOW: 'Waspada',
  RED: 'Kritis'
};

function formatRupiah(amount: number): string {
  return `Rp ${amount.toLocaleString('id-ID')}`;
}

@Component({
  selector: 'app-dashboard',
  imports: [AsyncPipe, MatIconModule, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class DashboardPage {
  private readonly authState = inject(AuthStateService);
  private readonly dashboardApi = inject(DashboardApiService);
  protected readonly menuList$ = this.authState.menuList$;

  private readonly userProfile = toSignal(this.authState.userProfile$, { initialValue: null });
  protected readonly isBm = () => this.userProfile()?.role?.toUpperCase() === 'BM';

  protected readonly summary = signal<DashboardSummary | null>(null);
  protected readonly loading = signal(false);

  protected readonly needsToReview = computed(() => this.summary()?.needsToReview ?? null);
  protected readonly loansInRegion = computed(() => {
    const value = this.summary()?.loansInRegion;
    return value != null ? formatRupiah(value) : null;
  });
  protected readonly activeBorrowers = computed(() => this.summary()?.activeBorrowers ?? null);
  protected readonly delinquentBorrowers = computed(() => this.summary()?.delinquentBorrowers ?? null);
  protected readonly loanStatus = computed<LoanStatusSlice[]>(() => this.summary()?.loanStatusDistribution ?? []);

  protected readonly nplPercent = computed(() => this.summary()?.nplPercent ?? null);
  protected readonly nplSeverity = computed(() => this.summary()?.nplSeverity ?? null);
  protected readonly nplSeverityLabel = computed(() => {
    const severity = this.nplSeverity();
    return severity ? NPL_SEVERITY_LABEL[severity] : null;
  });
  protected readonly totalOutstandingPenalty = computed(() => {
    const value = this.summary()?.totalOutstandingPenalty;
    return value != null ? formatRupiah(value) : null;
  });

  protected readonly donutBackground = computed(() => {
    const slices = this.loanStatus();
    if (!slices.length) return '#e6ebef';

    let cumulative = 0;
    const stops = slices.map((slice) => {
      const from = cumulative * 3.6;
      cumulative += slice.percent;
      const to = cumulative * 3.6;
      const color = SLICE_COLOR_BY_LABEL[slice.label] ?? FALLBACK_SLICE_COLOR;
      return `${color} ${from}deg ${to}deg`;
    });
    return `conic-gradient(${stops.join(', ')})`;
  });

  protected sliceColor(label: string): string {
    return SLICE_COLOR_BY_LABEL[label] ?? FALLBACK_SLICE_COLOR;
  }

  protected nplSeverityClass(): string {
    const severity = this.nplSeverity();
    return severity ? `npl-badge npl-${severity.toLowerCase()}` : 'npl-badge';
  }

  constructor() {
    effect(() => {
      if (!this.isBm() || this.summary() !== null || this.loading()) return;
      this.loading.set(true);
      this.dashboardApi.getSummary().subscribe({
        next: (data) => {
          this.summary.set(data);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
    });
  }
}
