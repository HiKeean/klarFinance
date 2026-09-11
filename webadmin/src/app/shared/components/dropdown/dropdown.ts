import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  HostListener,
  Input,
  forwardRef,
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { FormsModule } from '@angular/forms';

export interface DropdownOption<T = string | number> {
  label: string;
  value: T;
}

@Component({
  selector: 'app-dropdown',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dropdown.html',
  styleUrl: './dropdown.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => DropdownComponent),
      multi: true,
    },
  ],
})
export class DropdownComponent<T = string | number> implements ControlValueAccessor {
  @Input() placeholder = 'Select an option';
  @Input() search = false;
  @Input() data: DropdownOption<T>[] = [];

  open = false;
  query = '';
  disabled = false;
  selectedValue: T | null = null;
  panelStyle: Record<string, string> = {};

  private onChange: (value: T | null) => void = () => undefined;
  private onTouched: () => void = () => undefined;

  constructor(private readonly elementRef: ElementRef<HTMLElement>) {}

  get selectedOption(): DropdownOption<T> | undefined {
    return this.data.find((option) => option.value === this.selectedValue);
  }

  get filteredData(): DropdownOption<T>[] {
    const searchTerm = this.query.trim().toLowerCase();
    if (!searchTerm) return this.data;
    return this.data.filter((option) => option.label.toLowerCase().includes(searchTerm));
  }

  toggle(): void {
    if (this.disabled) return;
    this.open = !this.open;
    if (this.open) {
      this.onTouched();
      this.updatePanelPosition();
    }
  }

  select(option: DropdownOption<T>): void {
    this.selectedValue = option.value;
    this.query = '';
    this.open = false;
    this.onChange(option.value);
    this.onTouched();
  }

  clearSearch(): void {
    this.query = '';
  }

  writeValue(value: T | null): void {
    this.selectedValue = value;
  }

  registerOnChange(fn: (value: T | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  @HostListener('document:click', ['$event'])
  closeWhenClickedOutside(event: MouseEvent): void {
    if (!this.elementRef.nativeElement.contains(event.target as Node)) {
      this.open = false;
      this.query = '';
    }
  }

  @HostListener('window:resize')
  repositionOnResize(): void {
    if (this.open) this.updatePanelPosition();
  }

  private updatePanelPosition(): void {
    const trigger = this.elementRef.nativeElement.querySelector('.dropdown-trigger');
    if (!trigger) return;

    const rect = trigger.getBoundingClientRect();
    const panelHeight = this.search ? 220 : 180;
    const roomBelow = window.innerHeight - rect.bottom - 12;
    const opensAbove = roomBelow < Math.min(panelHeight, 180) && rect.top > panelHeight;
    const top = opensAbove ? Math.max(12, rect.top - panelHeight - 4) : rect.bottom + 4;

    this.panelStyle = {
      top: `${top}px`,
      left: `${rect.left}px`,
      width: `${rect.width}px`,
      'max-height': `${Math.max(80, Math.min(panelHeight, opensAbove ? rect.top - 16 : roomBelow))}px`,
    };
  }
}
