import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { LocationSelectComponent } from './location-select';
import { Dbo } from '../../../core/services/dbo/dbo';
import { ProvinceResponse, RegenciesResponse, DistrictsResponse, VillagesResponse } from '../../models/dbo-response';

const province: ProvinceResponse = { id: 1, name: 'Jawa Barat' };
const regency: RegenciesResponse = { id: 10, name: 'Bandung', province };
const district: DistrictsResponse = { id: 100, name: 'Coblong', regency };
const village: VillagesResponse = { id: 1000, name: 'Dago', district };

describe('LocationSelectComponent', () => {
  let fixture: ComponentFixture<LocationSelectComponent>;
  let component: LocationSelectComponent;
  let dbo: jasmine.SpyObj<Dbo>;

  beforeEach(async () => {
    dbo = jasmine.createSpyObj<Dbo>('Dbo', ['getAllProvinces', 'getAllRegencies', 'getAllDistricts', 'getAllVillages']);
    dbo.getAllProvinces.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [province] }));
    dbo.getAllRegencies.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [regency] }));
    dbo.getAllDistricts.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [district] }));
    dbo.getAllVillages.and.returnValue(of({ success: true, statusCode: 200, message: 'OK', data: [village] }));

    await TestBed.configureTestingModule({
      imports: [LocationSelectComponent],
      providers: [{ provide: Dbo, useValue: dbo }]
    }).compileComponents();

    fixture = TestBed.createComponent(LocationSelectComponent);
    component = fixture.componentInstance;
  });

  it('[positive] loads the province list on init', () => {
    fixture.detectChanges();

    expect(dbo.getAllProvinces).toHaveBeenCalledWith('');
    expect(component.province).toEqual([province]);
    expect(component.loadingProvince).toBeFalse();
  });

  it('[negative] clears the province list and stops loading when the initial load fails', () => {
    dbo.getAllProvinces.and.returnValue(throwError(() => new Error('network down')));

    fixture.detectChanges();

    expect(component.province).toEqual([]);
    expect(component.loadingProvince).toBeFalse();
  });

  describe('cascading selection', () => {
    beforeEach(() => fixture.detectChanges());

    it('[positive] selecting a province loads its regencies and resets deeper selections', () => {
      component.onProvinceSelected(province);

      expect(component.selectedProvince).toEqual(province);
      expect(dbo.getAllRegencies).toHaveBeenCalledWith(province.id, '');
      expect(component.regencies).toEqual([regency]);
      expect(component.selectedRegency).toBeNull();
      expect(component.selectedDistrict).toBeNull();
      expect(component.selectedVillage).toBeNull();
    });

    it('[negative] clearing the province (null) resets regencies/districts/villages without loading', () => {
      component.onProvinceSelected(province);
      dbo.getAllRegencies.calls.reset();

      component.onProvinceSelected(null);

      expect(component.selectedProvince).toBeNull();
      expect(component.regencies).toEqual([]);
      expect(dbo.getAllRegencies).not.toHaveBeenCalled();
    });

    it('[positive] selecting a regency loads its districts and resets deeper selections', () => {
      component.onProvinceSelected(province);

      component.onRegencySelected(regency);

      expect(component.selectedRegency).toEqual(regency);
      expect(dbo.getAllDistricts).toHaveBeenCalledWith(regency.id, '');
      expect(component.districts).toEqual([district]);
      expect(component.selectedDistrict).toBeNull();
      expect(component.selectedVillage).toBeNull();
    });

    it('[positive] selecting a district loads its villages and resets the selected village', () => {
      component.onProvinceSelected(province);
      component.onRegencySelected(regency);

      component.onDistrictSelected(district);

      expect(component.selectedDistrict).toEqual(district);
      expect(dbo.getAllVillages).toHaveBeenCalledWith(district.id, '');
      expect(component.villages).toEqual([village]);
      expect(component.selectedVillage).toBeNull();
    });

    it('[positive] selecting a village emits the full LocationValue via the CVA onChange callback', () => {
      const onChange = jasmine.createSpy('onChange');
      const onTouched = jasmine.createSpy('onTouched');
      component.registerOnChange(onChange);
      component.registerOnTouched(onTouched);
      component.onProvinceSelected(province);
      component.onRegencySelected(regency);
      component.onDistrictSelected(district);

      component.onVillageSelected(village);

      expect(component.selectedVillage).toEqual(village);
      expect(onChange).toHaveBeenCalledWith({
        provinceId: province.id,
        regencyId: regency.id,
        districtId: district.id,
        villageId: village.id
      });
      expect(onTouched).toHaveBeenCalled();
    });
  });

  describe('disabled getters', () => {
    beforeEach(() => fixture.detectChanges());

    it('[positive] regencyDisabled is true until a province is selected', () => {
      expect(component.regencyDisabled).toBeTrue();
      component.onProvinceSelected(province);
      expect(component.regencyDisabled).toBeFalse();
    });

    it('[positive] districtDisabled is true until a regency is selected', () => {
      component.onProvinceSelected(province);
      expect(component.districtDisabled).toBeTrue();
      component.onRegencySelected(regency);
      expect(component.districtDisabled).toBeFalse();
    });

    it('[positive] villageDisabled is true until a district is selected', () => {
      component.onProvinceSelected(province);
      component.onRegencySelected(regency);
      expect(component.villageDisabled).toBeTrue();
      component.onDistrictSelected(district);
      expect(component.villageDisabled).toBeFalse();
    });

    it('[negative] all are disabled when the component itself is disabled', () => {
      component.onProvinceSelected(province);
      component.onRegencySelected(regency);
      component.onDistrictSelected(district);
      component.setDisabledState(true);

      expect(component.regencyDisabled).toBeTrue();
      expect(component.districtDisabled).toBeTrue();
      expect(component.villageDisabled).toBeTrue();
    });
  });

  describe('display formatters', () => {
    it('[positive] each *ToString formatter returns the item name', () => {
      expect(component.provinceToString(province)).toBe('Jawa Barat');
      expect(component.regencyToString(regency)).toBe('Bandung');
      expect(component.districtToString(district)).toBe('Coblong');
      expect(component.villageToString(village)).toBe('Dago');
    });
  });

  describe('writeValue', () => {
    beforeEach(() => fixture.detectChanges());

    it('[negative] writing null clears the current selection', () => {
      component.onProvinceSelected(province);
      component.onRegencySelected(regency);

      component.writeValue(null);

      expect(component.selectedProvince).toBeNull();
      expect(component.selectedRegency).toBeNull();
      expect(component.regencies).toEqual([]);
    });
  });
});
