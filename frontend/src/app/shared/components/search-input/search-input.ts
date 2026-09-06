import { Component, DestroyRef, EventEmitter, inject, Input, Output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

/**
 * Search input reusable (debounce 300ms bawaan) - dipakai pertama kali di halaman Inquiry,
 * tapi sengaja gak digantung ke domain apapun biar bisa dipakai ulang di fitur lain.
 */
@Component({
  selector: 'app-search-input',
  imports: [FormsModule],
  templateUrl: './search-input.html',
  styleUrl: './search-input.css'
})
export class SearchInputComponent {
  @Input() placeholder = 'Cari…';
  @Output() readonly search = new EventEmitter<string>();

  private readonly destroyRef = inject(DestroyRef);
  private readonly queryChanges = new Subject<string>();

  protected readonly value = signal('');

  constructor() {
    const subscription = this.queryChanges.pipe(debounceTime(300), distinctUntilChanged()).subscribe((query) => {
      this.search.emit(query);
    });
    this.destroyRef.onDestroy(() => subscription.unsubscribe());
  }

  protected onInput(query: string): void {
    this.value.set(query);
    this.queryChanges.next(query.trim());
  }
}
