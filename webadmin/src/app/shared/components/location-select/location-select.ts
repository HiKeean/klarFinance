import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  forwardRef,
  inject,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, debounceTime, distinctUntilChanged, finalize, switchMap } from 'rxjs';

import { HlmComboboxImports } from '@spartan-ng/helm/combobox';

import {
  DistrictsResponse,
  ProvinceResponse,
  RegenciesResponse,
  VillagesResponse,
} from '../../models/dbo-response';

import { Dbo } from '../../../core/services/dbo/dbo';

export interface LocationValue {
  provinceId: number | null;
  regencyId: number | null;
  districtId: number | null;
  villageId: number | null;
}

@Component({
  selector: 'app-location-select',
  standalone: true,
  imports: [...HlmComboboxImports],
  templateUrl: './location-select.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => LocationSelectComponent),
      multi: true,
    },
  ],
})
export class LocationSelectComponent implements ControlValueAccessor, OnInit {
  private readonly dbo = inject(Dbo);
  private readonly destroyRef = inject(DestroyRef);

  // =========================================================
  // DATA
  // =========================================================

  province: ProvinceResponse[] = [];
  regencies: RegenciesResponse[] = [];
  districts: DistrictsResponse[] = [];
  villages: VillagesResponse[] = [];

  // =========================================================
  // SELECTED
  // =========================================================

  selectedProvince: ProvinceResponse | null = null;
  selectedRegency: RegenciesResponse | null = null;
  selectedDistrict: DistrictsResponse | null = null;
  selectedVillage: VillagesResponse | null = null;

  // =========================================================
  // LOADING
  // =========================================================

  loadingProvince = false;
  loadingRegencies = false;
  loadingDistricts = false;
  loadingVillages = false;

  // =========================================================
  // DISABLED
  // =========================================================

  disabled = false;

  get regencyDisabled(): boolean {
    return this.disabled || this.selectedProvince === null;
  }

  get districtDisabled(): boolean {
    return this.disabled || this.selectedRegency === null;
  }

  get villageDisabled(): boolean {
    return this.disabled || this.selectedDistrict === null;
  }

  // =========================================================
  // SEARCH SUBJECTS
  // =========================================================

  private readonly provinceSearch$ = new Subject<string>();
  private readonly regencySearch$ = new Subject<string>();
  private readonly districtSearch$ = new Subject<string>();
  private readonly villageSearch$ = new Subject<string>();

  // =========================================================
  // CVA
  // =========================================================

  private onChange: (value: LocationValue) => void = () => {};
  private onTouched: () => void = () => {};

  // =========================================================
  // INIT
  // =========================================================

  ngOnInit(): void {
    this.setupProvinceSearch();
    this.setupRegencySearch();
    this.setupDistrictSearch();
    this.setupVillageSearch();

    this.loadProvinces();
  }

  // =========================================================
  // PROVINCE SEARCH
  // =========================================================

  private setupProvinceSearch(): void {
    this.provinceSearch$
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),

        switchMap((search) => {
          this.loadingProvince = true;

          return this.dbo.getAllProvinces(search).pipe(
            finalize(() => {
              this.loadingProvince = false;
            }),
          );
        }),

        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (response) => {
          this.province = response.data;
        },
        error: (error) => {
          console.error('Failed to search provinces', error);
          this.province = [];
        },
      });
  }

  private loadProvinces(search = ''): void {
    this.loadingProvince = true;

    this.dbo
      .getAllProvinces(search)
      .pipe(
        finalize(() => {
          this.loadingProvince = false;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (response) => {
          this.province = response.data;
        },
        error: (error) => {
          console.error('Failed to load provinces', error);
          this.province = [];
        },
      });
  }

  onProvinceSearch(event: Event): void {
    const input = event.target as HTMLInputElement;

    this.provinceSearch$.next(input.value);
  }

  // =========================================================
  // PROVINCE SELECTED
  // =========================================================

  onProvinceSelected(province: ProvinceResponse | null | undefined): void {
    const value = province ?? null;

    this.selectedProvince = value;

    // Reset children
    this.selectedRegency = null;
    this.selectedDistrict = null;
    this.selectedVillage = null;

    this.regencies = [];
    this.districts = [];
    this.villages = [];

    if (value) {
      this.loadRegencies(value.id);
    }

    this.emitValue();
  }

  // =========================================================
  // REGENCY SEARCH
  // =========================================================

  private setupRegencySearch(): void {
    this.regencySearch$
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),

        switchMap((search) => {
          if (!this.selectedProvince) {
            return [];
          }

          this.loadingRegencies = true;

          return this.dbo.getAllRegencies(this.selectedProvince.id, search).pipe(
            finalize(() => {
              this.loadingRegencies = false;
            }),
          );
        }),

        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (response) => {
          if (response) {
            this.regencies = response.data;
          }
        },
        error: (error) => {
          console.error('Failed to search regencies', error);
          this.regencies = [];
        },
      });
  }

  private loadRegencies(provinceId: number, search = ''): void {
    this.loadingRegencies = true;

    this.dbo
      .getAllRegencies(provinceId, search)
      .pipe(
        finalize(() => {
          this.loadingRegencies = false;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (response) => {
          this.regencies = response.data;
        },
        error: (error) => {
          console.error('Failed to load regencies', error);
          this.regencies = [];
        },
      });
  }

  onRegencySearch(event: Event): void {
    const input = event.target as HTMLInputElement;

    this.regencySearch$.next(input.value);
  }

  // =========================================================
  // REGENCY SELECTED
  // =========================================================

  onRegencySelected(regency: RegenciesResponse | null | undefined): void {
    const value = regency ?? null;

    this.selectedRegency = value;

    // Reset children
    this.selectedDistrict = null;
    this.selectedVillage = null;

    this.districts = [];
    this.villages = [];

    if (value) {
      this.loadDistricts(value.id);
    }

    this.emitValue();
  }

  // =========================================================
  // DISTRICT SEARCH
  // =========================================================

  private setupDistrictSearch(): void {
    this.districtSearch$
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),

        switchMap((search) => {
          if (!this.selectedRegency) {
            return [];
          }

          this.loadingDistricts = true;

          return this.dbo.getAllDistricts(this.selectedRegency.id, search).pipe(
            finalize(() => {
              this.loadingDistricts = false;
            }),
          );
        }),

        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (response) => {
          if (response) {
            this.districts = response.data;
          }
        },
        error: (error) => {
          console.error('Failed to search districts', error);
          this.districts = [];
        },
      });
  }

  private loadDistricts(regencyId: number, search = ''): void {
    this.loadingDistricts = true;

    this.dbo
      .getAllDistricts(regencyId, search)
      .pipe(
        finalize(() => {
          this.loadingDistricts = false;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (response) => {
          this.districts = response.data;
        },
        error: (error) => {
          console.error('Failed to load districts', error);
          this.districts = [];
        },
      });
  }

  onDistrictSearch(event: Event): void {
    const input = event.target as HTMLInputElement;

    this.districtSearch$.next(input.value);
  }

  // =========================================================
  // DISTRICT SELECTED
  // =========================================================

  onDistrictSelected(district: DistrictsResponse | null | undefined): void {
    const value = district ?? null;

    this.selectedDistrict = value;

    // Reset village
    this.selectedVillage = null;
    this.villages = [];

    if (value) {
      this.loadVillages(value.id);
    }

    this.emitValue();
  }

  // =========================================================
  // VILLAGE SEARCH
  // =========================================================

  private setupVillageSearch(): void {
    this.villageSearch$
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),

        switchMap((search) => {
          if (!this.selectedDistrict) {
            return [];
          }

          this.loadingVillages = true;

          return this.dbo.getAllVillages(this.selectedDistrict.id, search).pipe(
            finalize(() => {
              this.loadingVillages = false;
            }),
          );
        }),

        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (response) => {
          if (response) {
            this.villages = response.data;
          }
        },
        error: (error) => {
          console.error('Failed to search villages', error);
          this.villages = [];
        },
      });
  }

  private loadVillages(districtId: number, search = ''): void {
    this.loadingVillages = true;

    this.dbo
      .getAllVillages(districtId, search)
      .pipe(
        finalize(() => {
          this.loadingVillages = false;
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (response) => {
          this.villages = response.data;
        },
        error: (error) => {
          console.error('Failed to load villages', error);
          this.villages = [];
        },
      });
  }

  onVillageSearch(event: Event): void {
    const input = event.target as HTMLInputElement;

    this.villageSearch$.next(input.value);
  }

  // =========================================================
  // VILLAGE SELECTED
  // =========================================================

  onVillageSelected(village: VillagesResponse | null | undefined): void {
    this.selectedVillage = village ?? null;

    this.emitValue();
  }

  // =========================================================
  // DISPLAY
  // =========================================================

  provinceToString = (item: ProvinceResponse): string => item?.name ?? '';

  regencyToString = (item: RegenciesResponse): string => item?.name ?? '';

  districtToString = (item: DistrictsResponse): string => item?.name ?? '';

  villageToString = (item: VillagesResponse): string => item?.name ?? '';

  // =========================================================
  // CONTROL VALUE ACCESSOR
  // =========================================================

  writeValue(value: LocationValue | null): void {
    if (!value) {
      this.clearLocation();
      return;
    }

    /*
     * IMPORTANT:
     *
     * Your current Dbo service only has:
     *
     * getAllProvinces(search)
     * getAllRegencies(provinceId, search)
     * getAllDistricts(regencyId, search)
     * getAllVillages(districtId, search)
     *
     * It does not have getById().
     *
     * Therefore we should NOT pretend we can restore
     * ProvinceResponse / RegencyResponse / etc. from IDs here.
     *
     * Add getProvinceById(), getRegencyById(), etc.
     * if edit-form value restoration is required.
     */
  }

  registerOnChange(fn: (value: LocationValue) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  // =========================================================
  // EMIT
  // =========================================================

  private emitValue(): void {
    this.onChange({
      provinceId: this.selectedProvince?.id ?? null,
      regencyId: this.selectedRegency?.id ?? null,
      districtId: this.selectedDistrict?.id ?? null,
      villageId: this.selectedVillage?.id ?? null,
    });

    this.onTouched();
  }

  // =========================================================
  // CLEAR
  // =========================================================

  private clearLocation(): void {
    this.selectedProvince = null;
    this.selectedRegency = null;
    this.selectedDistrict = null;
    this.selectedVillage = null;

    this.regencies = [];
    this.districts = [];
    this.villages = [];
  }
}
