import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { HlmFieldImports } from '@spartan-ng/helm/field';
import { HlmInputImports } from '@spartan-ng/helm/input';

export type AppInputType = 'text' | 'password' | 'email' | 'number' | 'tel' | 'search' | 'date';

@Component({
  selector: 'app-input',
  standalone: true,
  imports: [ReactiveFormsModule, ...HlmInputImports, ...HlmFieldImports],
  templateUrl: './input.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InputComponent {
  private static nextId = 0;

  readonly inputId = `app-input-${InputComponent.nextId++}`;

  @Input({ required: true }) control!: FormControl;
  @Input() label = '';
  @Input() type: AppInputType = 'text';
  @Input() placeholder = '';
  @Input() hint = '';
  @Input() required = false;
  @Input() autocomplete = 'off';

  /**
   * Optional overrides keyed by validator name (e.g. 'required', 'email',
   * 'minlength'). Falls back to a sensible Indonesian default message per
   * validator when not provided.
   */
  @Input() errorMessages: Record<string, string> = {};

  get isInvalid(): boolean {
    return this.control.invalid && (this.control.touched || this.control.dirty);
  }

  get errorMessage(): string {
    const errors = this.control.errors;
    if (!errors) return '';

    const key = Object.keys(errors)[0];
    return this.errorMessages[key] ?? this.defaultMessage(key, errors[key]);
  }

  private defaultMessage(key: string, errorValue: unknown): string {
    switch (key) {
      case 'required':
        return `${this.label || 'Kolom ini'} wajib diisi`;
      case 'email':
        return 'Format email tidak valid';
      case 'minlength':
        return `Minimal ${(errorValue as { requiredLength: number }).requiredLength} karakter`;
      case 'maxlength':
        return `Maksimal ${(errorValue as { requiredLength: number }).requiredLength} karakter`;
      case 'pattern':
        return 'Format tidak sesuai';
      default:
        return 'Nilai tidak valid';
    }
  }
}
