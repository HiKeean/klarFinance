import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, Validators } from '@angular/forms';
import { InputComponent } from './input';

describe('InputComponent', () => {
  let fixture: ComponentFixture<InputComponent>;
  let component: InputComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [InputComponent] }).compileComponents();

    fixture = TestBed.createComponent(InputComponent);
    component = fixture.componentInstance;
  });

  function withControl(control: FormControl): void {
    component.control = control;
    fixture.detectChanges();
  }

  it('should be created', () => {
    withControl(new FormControl(''));
    expect(component).toBeTruthy();
  });

  it('[positive] assigns each instance a unique inputId', () => {
    withControl(new FormControl(''));
    const fixture2 = TestBed.createComponent(InputComponent);
    fixture2.componentInstance.control = new FormControl('');
    fixture2.detectChanges();

    expect(component.inputId).not.toBe(fixture2.componentInstance.inputId);
  });

  describe('isInvalid', () => {
    it('[negative] is false when the control is invalid but untouched/pristine', () => {
      withControl(new FormControl('', Validators.required));
      expect(component.isInvalid).toBeFalse();
    });

    it('[positive] is true once the control is invalid and touched', () => {
      const control = new FormControl('', Validators.required);
      control.markAsTouched();
      withControl(control);

      expect(component.isInvalid).toBeTrue();
    });

    it('[positive] is true once the control is invalid and dirty', () => {
      const control = new FormControl('', Validators.required);
      control.markAsDirty();
      withControl(control);

      expect(component.isInvalid).toBeTrue();
    });

    it('[negative] is false when the control is valid', () => {
      const control = new FormControl('ok', Validators.required);
      control.markAsTouched();
      withControl(control);

      expect(component.isInvalid).toBeFalse();
    });
  });

  describe('errorMessage', () => {
    it('[negative] returns an empty string when there are no errors', () => {
      withControl(new FormControl('ok', Validators.required));
      expect(component.errorMessage).toBe('');
    });

    it('[positive] uses the default Indonesian "required" message including the label', () => {
      component.label = 'Nama';
      withControl(new FormControl('', Validators.required));

      expect(component.errorMessage).toBe('Nama wajib diisi');
    });

    it('[positive] falls back to "Kolom ini" when no label is set for required', () => {
      withControl(new FormControl('', Validators.required));
      expect(component.errorMessage).toBe('Kolom ini wajib diisi');
    });

    it('[positive] uses the default "email" message', () => {
      withControl(new FormControl('not-an-email', Validators.email));
      expect(component.errorMessage).toBe('Format email tidak valid');
    });

    it('[positive] uses the default "minlength" message with the required length', () => {
      withControl(new FormControl('ab', Validators.minLength(5)));
      expect(component.errorMessage).toBe('Minimal 5 karakter');
    });

    it('[positive] uses the default "maxlength" message with the required length', () => {
      withControl(new FormControl('abcdef', Validators.maxLength(3)));
      expect(component.errorMessage).toBe('Maksimal 3 karakter');
    });

    it('[positive] uses the default "pattern" message', () => {
      withControl(new FormControl('abc', Validators.pattern(/^\d+$/)));
      expect(component.errorMessage).toBe('Format tidak sesuai');
    });

    it('[positive] falls back to a generic message for an unrecognized validator key', () => {
      const control = new FormControl('x');
      control.setErrors({ customRule: true });
      withControl(control);

      expect(component.errorMessage).toBe('Nilai tidak valid');
    });

    it('[positive] prefers a caller-supplied override over the default message', () => {
      component.errorMessages = { required: 'Wajib diisi ya!' };
      withControl(new FormControl('', Validators.required));

      expect(component.errorMessage).toBe('Wajib diisi ya!');
    });
  });
});
