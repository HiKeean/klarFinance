import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output, forwardRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import type { BrnDialogState } from '@spartan-ng/brain/dialog';
import { HlmButtonImports } from '@spartan-ng/helm/button';
import { HlmDialogImports } from '@spartan-ng/helm/dialog';

export type AppModalSize = 'sm' | 'md' | 'lg';

/**
 * Reusable add/edit modal: title + close (X) in the header, custom content
 * via <ng-content>, and a Cancel/Submit footer. Open state is a
 * ControlValueAccessor<boolean>, so bind it with [(ngModel)] (or a
 * FormControl) instead of an @Input/@Output pair.
 */
@Component({
  selector: 'app-modal',
  standalone: true,
  imports: [...HlmDialogImports, ...HlmButtonImports],
  templateUrl: './modal.html',
  styleUrl: './modal.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ModalComponent),
      multi: true,
    },
  ],
})
export class ModalComponent implements ControlValueAccessor {
  @Input() title = '';
  @Input() size: AppModalSize = 'sm';
  @Input() submitLabel = 'Save';
  @Input() cancelLabel = 'Cancel';
  @Input() submitDisabled = false;
  /** Blocks the X button, Cancel button, backdrop click and Escape (e.g. while saving). */
  @Input() disableClose = false;

  /** Fired when Submit is clicked; the caller owns the save + closing logic. */
  @Output() submitted = new EventEmitter<void>();

  open = false;

  private onChange: (value: boolean) => void = () => undefined;
  private onTouched: () => void = () => undefined;

  get state(): BrnDialogState {
    return this.open ? 'open' : 'closed';
  }

  onStateChanged(state: BrnDialogState): void {
    const isOpen = state === 'open';
    if (isOpen === this.open) return;
    this.open = isOpen;
    if (!isOpen) {
      this.onTouched();
      this.onChange(false);
    }
  }

  onCancelClick(): void {
    if (this.disableClose || !this.open) return;
    this.open = false;
    this.onTouched();
    this.onChange(false);
  }

  onSubmitClick(): void {
    this.submitted.emit();
  }

  writeValue(value: boolean | null): void {
    this.open = !!value;
  }

  registerOnChange(fn: (value: boolean) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(): void {
    // Modals don't have a meaningful "disabled" visual state; no-op.
  }
}
