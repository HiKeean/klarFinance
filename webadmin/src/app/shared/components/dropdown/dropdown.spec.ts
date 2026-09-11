import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DropdownComponent, DropdownOption } from './dropdown';

const options: DropdownOption<string>[] = [
  { label: 'Apple', value: 'apple' },
  { label: 'Banana', value: 'banana' },
  { label: 'Cherry', value: 'cherry' }
];

describe('DropdownComponent', () => {
  let fixture: ComponentFixture<DropdownComponent<string>>;
  let component: DropdownComponent<string>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [DropdownComponent] }).compileComponents();

    fixture = TestBed.createComponent<DropdownComponent<string>>(DropdownComponent);
    component = fixture.componentInstance;
    component.data = options;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });

  describe('filteredData', () => {
    it('[positive] returns all options when the query is empty', () => {
      expect(component.filteredData).toEqual(options);
    });

    it('[positive] filters case-insensitively by label substring', () => {
      component.query = 'an';
      expect(component.filteredData).toEqual([options[1]]);
    });

    it('[negative] returns an empty list when nothing matches', () => {
      component.query = 'zzz';
      expect(component.filteredData).toEqual([]);
    });

    it('[positive] trims whitespace from the query before filtering', () => {
      component.query = '  cherry  ';
      expect(component.filteredData).toEqual([options[2]]);
    });
  });

  describe('selectedOption', () => {
    it('[positive] resolves the option matching selectedValue', () => {
      component.selectedValue = 'banana';
      expect(component.selectedOption).toEqual(options[1]);
    });

    it('[negative] is undefined when selectedValue matches nothing', () => {
      component.selectedValue = 'durian' as any;
      expect(component.selectedOption).toBeUndefined();
    });
  });

  describe('toggle', () => {
    it('[positive] opens the panel when closed', () => {
      component.toggle();
      expect(component.open).toBeTrue();
    });

    it('[positive] closes the panel when open', () => {
      component.open = true;
      component.toggle();
      expect(component.open).toBeFalse();
    });

    it('[negative] does nothing while disabled', () => {
      component.disabled = true;
      component.toggle();
      expect(component.open).toBeFalse();
    });

    it('[positive] calls onTouched when opening', () => {
      const onTouched = jasmine.createSpy('onTouched');
      component.registerOnTouched(onTouched);

      component.toggle();

      expect(onTouched).toHaveBeenCalled();
    });
  });

  describe('select', () => {
    it('[positive] sets the value, closes the panel, clears the query, and notifies the CVA callbacks', () => {
      const onChange = jasmine.createSpy('onChange');
      const onTouched = jasmine.createSpy('onTouched');
      component.registerOnChange(onChange);
      component.registerOnTouched(onTouched);
      component.query = 'ban';
      component.open = true;

      component.select(options[1]);

      expect(component.selectedValue).toBe('banana');
      expect(component.query).toBe('');
      expect(component.open).toBeFalse();
      expect(onChange).toHaveBeenCalledWith('banana');
      expect(onTouched).toHaveBeenCalled();
    });
  });

  describe('ControlValueAccessor', () => {
    it('[positive] writeValue sets selectedValue', () => {
      component.writeValue('cherry');
      expect(component.selectedValue).toBe('cherry');
    });

    it('[positive] setDisabledState toggles disabled', () => {
      component.setDisabledState(true);
      expect(component.disabled).toBeTrue();
    });
  });

  describe('closeWhenClickedOutside', () => {
    it('[positive] closes the panel and clears the query on an outside click', () => {
      component.open = true;
      component.query = 'ap';
      const outsideEl = document.createElement('div');
      document.body.appendChild(outsideEl);

      component.closeWhenClickedOutside({ target: outsideEl } as unknown as MouseEvent);

      expect(component.open).toBeFalse();
      expect(component.query).toBe('');
      document.body.removeChild(outsideEl);
    });

    it('[negative] leaves the panel open on a click inside the component', () => {
      component.open = true;
      const insideEl = fixture.nativeElement as HTMLElement;

      component.closeWhenClickedOutside({ target: insideEl } as unknown as MouseEvent);

      expect(component.open).toBeTrue();
    });
  });

  describe('clearSearch', () => {
    it('[positive] resets the query to an empty string', () => {
      component.query = 'something';
      component.clearSearch();
      expect(component.query).toBe('');
    });
  });
});
