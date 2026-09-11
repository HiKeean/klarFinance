import { Component, ElementRef, OnDestroy, ViewChild, afterNextRender, computed, inject, signal } from '@angular/core';
import type { Feature, FeatureCollection } from 'geojson';
import * as L from 'leaflet';
import { TableComponent } from '../../../../shared/components/table/table';
import { TableCellDirective } from '../../../../shared/components/table/table-cell.directive';
import { TableColumn } from '../../../../shared/components/table/table-column';
import { NplReportServices } from '../../services/npl-report-services';
import { NplReportItem, NplSeverity } from '../../models/npl-report.model';

const SEVERITY_COLOR: Record<NplSeverity, string> = {
  GREEN: '#1f9d55',
  YELLOW: '#c67a1f',
  RED: '#c64444',
};
const NO_DATA_COLOR = '#c9d2d8';
const SEVERITY_LABEL: Record<NplSeverity, string> = {
  GREEN: 'Sehat (<2%)',
  YELLOW: 'Waspada (2-5%)',
  RED: 'Kritis (>5%)',
};
const SEVERITY_ORDER: Record<NplSeverity, number> = { RED: 0, YELLOW: 1, GREEN: 2 };

function formatRupiah(amount: number): string {
  return `Rp ${amount.toLocaleString('id-ID')}`;
}

@Component({
  selector: 'app-npl-report',
  standalone: true,
  imports: [TableComponent, TableCellDirective],
  templateUrl: './npl-report.html',
  styleUrl: './npl-report.css',
})
export class NplReportPage implements OnDestroy {
  private readonly nplReportServices = inject(NplReportServices);

  @ViewChild('mapContainer') private mapContainer?: ElementRef<HTMLDivElement>;

  readonly items = signal<NplReportItem[]>([]);
  readonly isLoading = signal(true);
  readonly errorMessage = signal('');
  readonly mapError = signal('');

  private map?: L.Map;
  private geoLayer?: L.GeoJSON;

  readonly summary = computed(() => {
    const rows = this.items();
    return {
      green: rows.filter((r) => r.nplSeverity === 'GREEN').length,
      yellow: rows.filter((r) => r.nplSeverity === 'YELLOW').length,
      red: rows.filter((r) => r.nplSeverity === 'RED').length,
    };
  });

  readonly sortedBySeverity = computed(() =>
    [...this.items()].sort(
      (a, b) => SEVERITY_ORDER[a.nplSeverity] - SEVERITY_ORDER[b.nplSeverity] || b.nplPercent - a.nplPercent
    )
  );

  readonly columns: TableColumn<NplReportItem>[] = [
    { key: 'branchName', header: 'Branch', width: '25%' },
    { key: 'nplSeverity', header: 'Status', width: '15%' },
    { key: 'nplPercent', header: 'NPL %', width: '10%' },
    { key: 'activeLoanCount', header: 'Pinjaman Aktif', width: '15%' },
    { key: 'overdueLoanCount', header: 'Pinjaman Overdue', width: '15%' },
    { key: 'totalOutstandingPenalty', header: 'Total Denda', width: '20%' },
  ];

  readonly trackById = (row: NplReportItem) => row.branchId;

  constructor() {
    afterNextRender(() => {
      this.initMap();
      this.load();
    });
  }

  ngOnDestroy(): void {
    this.map?.remove();
  }

  severityLabel(severity: NplSeverity): string {
    return SEVERITY_LABEL[severity];
  }

  formatPenalty(amount: number): string {
    return formatRupiah(amount);
  }

  private load(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.nplReportServices.getReport().subscribe({
      next: (response) => {
        if (response.success) {
          this.items.set(response.data ?? []);
          this.colorizeMap();
        } else {
          this.errorMessage.set(response.message || 'Gagal memuat NPL report.');
        }
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Gagal memuat NPL report.');
        this.isLoading.set(false);
      },
    });
  }

  private initMap(): void {
    if (!this.mapContainer) return;
    this.map = L.map(this.mapContainer.nativeElement).setView([-2.5, 118], 5);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors',
      maxZoom: 18,
    }).addTo(this.map);

    fetch('/geo/indonesia-regencies.geojson')
      .then((res) => res.json())
      .then((geojson: FeatureCollection) => {
        this.geoLayer = L.geoJSON(geojson, {
          style: () => ({ fillColor: NO_DATA_COLOR, fillOpacity: 0.75, color: '#fff', weight: 1 }),
        }).addTo(this.map!);
        this.colorizeMap();
      })
      .catch(() => this.mapError.set('Gagal memuat data peta wilayah.'));
  }

  private regencyToBranch(): Map<number, NplReportItem> {
    const lookup = new Map<number, NplReportItem>();
    for (const item of this.items()) {
      for (const regencyId of item.regencyIds) lookup.set(regencyId, item);
    }
    return lookup;
  }

  private colorizeMap(): void {
    if (!this.geoLayer || !this.items().length) return;
    const lookup = this.regencyToBranch();

    this.geoLayer.eachLayer((layer) => {
      const geoLayer = layer as L.Path & { feature?: Feature };
      const properties = (geoLayer.feature?.properties ?? {}) as Record<string, unknown>;
      const regencyCode = properties['regencyCode'] as number | undefined;
      const item = regencyCode != null ? lookup.get(regencyCode) : undefined;
      const name = (properties['name'] as string | undefined) ?? 'Wilayah';

      geoLayer.setStyle({ fillColor: item ? SEVERITY_COLOR[item.nplSeverity] : NO_DATA_COLOR, fillOpacity: 0.75, color: '#fff', weight: 1 });
      geoLayer.bindPopup(
        item
          ? `<strong>${name}</strong><br>Branch: ${item.branchName}<br>NPL: ${item.nplPercent}%`
          : `<strong>${name}</strong><br>Belum ada data pinjaman`
      );
    });
  }
}
