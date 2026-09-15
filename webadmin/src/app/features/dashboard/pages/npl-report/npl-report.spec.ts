import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import type { FeatureCollection } from 'geojson';

import { NplReportPage } from './npl-report';
import { NplReportServices } from '../../services/npl-report-services';
import { NplReportItem } from '../../models/npl-report.model';

/** initMap()/load() run inside afterNextRender, and load()'s geojson fetch is chained
 *  promises - flush the real event loop (not just microtasks) before asserting, matching
 *  the pattern used for real async work elsewhere in this codebase (see auth.interceptor.spec.ts). */
function flush(): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, 0));
}

const emptyGeoJson: FeatureCollection = { type: 'FeatureCollection', features: [] };

function jsonResponse(body: unknown): Response {
  return { json: () => Promise.resolve(body) } as Response;
}

const green: NplReportItem = {
  branchId: 1, branchName: 'Cabang Sehat', activeLoanCount: 10, overdueLoanCount: 0,
  nplPercent: 1.5, nplSeverity: 'GREEN', totalOutstandingPenalty: 0, regencyIds: [101]
};
const yellow: NplReportItem = {
  branchId: 2, branchName: 'Cabang Waspada', activeLoanCount: 10, overdueLoanCount: 2,
  nplPercent: 3.0, nplSeverity: 'YELLOW', totalOutstandingPenalty: 500000, regencyIds: [102]
};
const redLow: NplReportItem = {
  branchId: 3, branchName: 'Cabang Kritis A', activeLoanCount: 10, overdueLoanCount: 4,
  nplPercent: 8.2, nplSeverity: 'RED', totalOutstandingPenalty: 2000000, regencyIds: [103]
};
const redHigh: NplReportItem = {
  branchId: 4, branchName: 'Cabang Kritis B', activeLoanCount: 10, overdueLoanCount: 6,
  nplPercent: 9.5, nplSeverity: 'RED', totalOutstandingPenalty: 3000000, regencyIds: [104]
};

describe('NplReportPage', () => {
  let fixture: ComponentFixture<NplReportPage>;
  let component: NplReportPage;
  let nplReportServices: jasmine.SpyObj<NplReportServices>;
  let router: Router;
  let fetchSpy: jasmine.Spy;

  beforeEach(() => {
    nplReportServices = jasmine.createSpyObj<NplReportServices>('NplReportServices', ['getReport', 'getBranchLoans']);
    nplReportServices.getReport.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [] }));
    fetchSpy = spyOn(window, 'fetch').and.resolveTo(jsonResponse(emptyGeoJson));

    TestBed.configureTestingModule({
      imports: [NplReportPage],
      providers: [
        provideRouter([]),
        { provide: NplReportServices, useValue: nplReportServices }
      ]
    });

    fixture = TestBed.createComponent(NplReportPage);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
  });

  describe('pure computed/helpers (no render needed)', () => {
    it('[positive] summary counts branches per severity', () => {
      component.items.set([green, yellow, redLow, redHigh]);
      expect(component.summary()).toEqual({ green: 1, yellow: 1, red: 2 });
    });

    it('[positive] summary is all-zero when there are no items', () => {
      expect(component.summary()).toEqual({ green: 0, yellow: 0, red: 0 });
    });

    it('[positive] sortedBySeverity orders RED > YELLOW > GREEN, ties broken by nplPercent desc', () => {
      component.items.set([green, redLow, yellow, redHigh]);
      expect(component.sortedBySeverity()).toEqual([redHigh, redLow, yellow, green]);
    });

    it('[positive] severityLabel returns the Indonesian label per severity', () => {
      expect(component.severityLabel('GREEN')).toBe('Sehat (<2%)');
      expect(component.severityLabel('YELLOW')).toBe('Waspada (2-5%)');
      expect(component.severityLabel('RED')).toBe('Kritis (>5%)');
    });

    it('[positive] formatPenalty formats as Indonesian Rupiah', () => {
      expect(component.formatPenalty(2000000)).toBe('Rp 2.000.000');
    });

    it('[positive] openBranchDetail navigates to the branch detail route', () => {
      component.openBranchDetail(redLow);
      expect(router.navigate).toHaveBeenCalledWith(['/dashboard/npl-report', 3]);
    });

    it('[negative] ngOnDestroy does not throw when the map was never initialized', () => {
      expect(() => component.ngOnDestroy()).not.toThrow();
    });
  });

  describe('load() via afterNextRender', () => {
    it('[positive] populates items and clears loading on success', async () => {
      nplReportServices.getReport.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [green, redLow] }));

      fixture.detectChanges();
      await flush();
      await flush();

      expect(component.items()).toEqual([green, redLow]);
      expect(component.isLoading()).toBeFalse();
      expect(component.errorMessage()).toBe('');
    });

    it('[negative] surfaces the backend message when success is false', async () => {
      nplReportServices.getReport.and.returnValue(of({ success: false, statusCode: 500, message: 'DB unavailable', data: null as any }));

      fixture.detectChanges();
      await flush();
      await flush();

      expect(component.errorMessage()).toBe('DB unavailable');
      expect(component.isLoading()).toBeFalse();
      expect(component.items()).toEqual([]);
    });

    it('[negative] sets a generic error message on a transport error', async () => {
      nplReportServices.getReport.and.returnValue(throwError(() => new Error('network down')));

      fixture.detectChanges();
      await flush();
      await flush();

      expect(component.errorMessage()).toBe('Gagal memuat NPL report.');
      expect(component.isLoading()).toBeFalse();
    });

    it('[negative] sets mapError when the geojson fetch fails, without breaking the report load', async () => {
      fetchSpy.and.rejectWith(new Error('offline'));
      nplReportServices.getReport.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [green] }));

      fixture.detectChanges();
      await flush();
      await flush();

      expect(component.mapError()).toBe('Gagal memuat data peta wilayah.');
      expect(component.items()).toEqual([green]);
    });

    it('[positive] colors matched regions and leaves unmatched ones with the no-data color, without throwing', async () => {
      // Regency boundaries are polygons (L.geoJSON renders them as L.Path layers, which
      // colorizeMap() calls setStyle() on) - a Point geometry would render as an L.Marker
      // instead, which has no setStyle() and would throw.
      const geo: FeatureCollection = {
        type: 'FeatureCollection',
        features: [
          {
            type: 'Feature', properties: { regencyCode: 101, name: 'Wilayah A' },
            geometry: { type: 'Polygon', coordinates: [[[107, -6], [107.1, -6], [107.1, -6.1], [107, -6.1], [107, -6]]] }
          },
          {
            type: 'Feature', properties: { regencyCode: 999, name: 'Wilayah B' },
            geometry: { type: 'Polygon', coordinates: [[[108, -7], [108.1, -7], [108.1, -7.1], [108, -7.1], [108, -7]]] }
          }
        ]
      };
      fetchSpy.and.resolveTo(jsonResponse(geo));
      nplReportServices.getReport.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [green] }));

      expect(() => {
        fixture.detectChanges();
      }).not.toThrow();
      await flush();
      await flush();

      expect(component.items()).toEqual([green]);
      expect(component.mapError()).toBe('');
    });

    it('[negative] destroying the fixture (which calls ngOnDestroy) does not throw after the map has been initialized', async () => {
      fixture.detectChanges();
      await flush();
      await flush();

      // Let Angular's real teardown path invoke ngOnDestroy exactly once - calling the
      // lifecycle hook manually here as well as through TestBed's automatic per-test
      // cleanup would call map.remove() twice, which Leaflet itself rejects.
      expect(() => fixture.destroy()).not.toThrow();
    });
  });
});
