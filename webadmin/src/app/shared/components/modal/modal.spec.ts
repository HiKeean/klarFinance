import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ModalComponent } from './modal';

describe('ModalComponent', () => {
  let fixture: ComponentFixture<ModalComponent>;
  let component: ModalComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ModalComponent] }).compileComponents();

    fixture = TestBed.createComponent(ModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created, closed by default', () => {
    expect(component).toBeTruthy();
    expect(component.open).toBeFalse();
    expect(component.state).toBe('closed');
  });

  describe('ControlValueAccessor', () => {
    it('[positive] writeValue(true) opens the modal', () => {
      component.writeValue(true);
      expect(component.open).toBeTrue();
      expect(component.state).toBe('open');
    });

    it('[negative] writeValue(null) treats it as closed', () => {
      component.writeValue(null);
      expect(component.open).toBeFalse();
    });
  });

  describe('onStateChanged', () => {
    it('[positive] transitioning to "open" sets open=true without firing callbacks', () => {
      const onChange = jasmine.createSpy('onChange');
      const onTouched = jasmine.createSpy('onTouched');
      component.registerOnChange(onChange);
      component.registerOnTouched(onTouched);

      component.onStateChanged('open');

      expect(component.open).toBeTrue();
      expect(onChange).not.toHaveBeenCalled();
    });

    it('[positive] transitioning to "closed" sets open=false and fires onTouched/onChange(false)', () => {
      const onChange = jasmine.createSpy('onChange');
      const onTouched = jasmine.createSpy('onTouched');
      component.registerOnChange(onChange);
      component.registerOnTouched(onTouched);
      component.writeValue(true);

      component.onStateChanged('closed');

      expect(component.open).toBeFalse();
      expect(onChange).toHaveBeenCalledWith(false);
      expect(onTouched).toHaveBeenCalled();
    });

    it('[negative] is a no-op when the state does not actually change', () => {
      const onChange = jasmine.createSpy('onChange');
      component.registerOnChange(onChange);

      component.onStateChanged('closed');

      expect(onChange).not.toHaveBeenCalled();
    });
  });

  describe('onCancelClick', () => {
    it('[positive] closes the modal and notifies callbacks when open', () => {
      const onChange = jasmine.createSpy('onChange');
      component.registerOnChange(onChange);
      component.writeValue(true);

      component.onCancelClick();

      expect(component.open).toBeFalse();
      expect(onChange).toHaveBeenCalledWith(false);
    });

    it('[negative] does nothing when disableClose is true', () => {
      component.disableClose = true;
      component.writeValue(true);

      component.onCancelClick();

      expect(component.open).toBeTrue();
    });

    it('[negative] does nothing when already closed', () => {
      const onChange = jasmine.createSpy('onChange');
      component.registerOnChange(onChange);

      component.onCancelClick();

      expect(onChange).not.toHaveBeenCalled();
    });
  });

  describe('onSubmitClick', () => {
    it('[positive] emits the submitted event', () => {
      const spy = jasmine.createSpy('submitted');
      component.submitted.subscribe(spy);

      component.onSubmitClick();

      expect(spy).toHaveBeenCalled();
    });
  });
});
