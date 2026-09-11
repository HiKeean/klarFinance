import { computed, Directive, effect, input, untracked } from '@angular/core';
import { injectCustomClassSettable } from '@spartan-ng/brain/core';
import { BrnDialogOverlay } from '@spartan-ng/brain/dialog';
import { hlm } from '@spartan-ng/helm/utils';
import type { ClassValue } from 'clsx';

export const hlmDialogOverlayClass = hlm(
  // bg-black/10 (default Spartan "nova" style) nyaris gak kelihatan sebagai dim backdrop -
  // dinaikkan ke 50% biar modal punya pemisah visual yang jelas dari halaman di belakangnya
  // (laporan user: "modal jelek, backgroundnya hilang" - root cause: dim terlalu tipis, bukan
  // bug transparansi beneran, lihat komponen ModalComponent yang bg-nya sudah benar).
  'data-open:animate-in data-closed:animate-out data-closed:fade-out-0 data-open:fade-in-0 isolate bg-black/50 duration-100 supports-backdrop-filter:backdrop-blur-xs',
);

@Directive({
  selector: '[hlmDialogOverlay],hlm-dialog-overlay',
  hostDirectives: [BrnDialogOverlay],
})
export class HlmDialogOverlay {
  private readonly _classSettable = injectCustomClassSettable({ optional: true, host: true });

  public readonly userClass = input<ClassValue>('', { alias: 'class' });
  protected readonly _computedClass = computed(() => hlm(hlmDialogOverlayClass, this.userClass()));

  constructor() {
    effect(() => {
      const newClass = this._computedClass();
      untracked(() => this._classSettable?.setClassToCustomElement(newClass));
    });
  }
}
