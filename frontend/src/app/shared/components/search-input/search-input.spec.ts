import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SearchInputComponent } from './search-input';

/** debounceTime(300) is a plain RxJS timer with no crypto involved - a real event-loop wait past
 *  the debounce window works the same way the repo's existing flush() helpers wait for async work. */
function flush(ms = 350): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

describe('SearchInputComponent', () => {
  let fixture: ComponentFixture<SearchInputComponent>;
  let component: SearchInputComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [] });
    fixture = TestBed.createComponent(SearchInputComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('[positive] placeholder defaults to "Cari…"', () => {
    expect(component.placeholder).toBe('Cari…');
  });

  it('[positive] placeholder can be overridden via @Input', () => {
    component.placeholder = 'Cari nasabah…';
    expect(component.placeholder).toBe('Cari nasabah…');
  });

  it('[positive] typing emits search only after the debounce window', async () => {
    const emitted: string[] = [];
    component.search.subscribe((q) => emitted.push(q));

    (component as any).onInput('budi');
    expect(emitted).toEqual([]);

    await flush();
    expect(emitted).toEqual(['budi']);
  });

  it('[negative] identical consecutive values (after trim) do not re-emit', async () => {
    const emitted: string[] = [];
    component.search.subscribe((q) => emitted.push(q));

    (component as any).onInput('budi');
    await flush();
    (component as any).onInput('budi ');
    await flush();

    expect(emitted).toEqual(['budi']);
  });

  it('[positive] the value signal reflects the raw (untrimmed) input', () => {
    (component as any).onInput('  budi  ');
    expect((component as any).value()).toBe('  budi  ');
  });

  it('[negative] destroying the component unsubscribes so no emission fires after destroy', async () => {
    const emitted: string[] = [];
    component.search.subscribe((q) => emitted.push(q));

    (component as any).onInput('budi');
    fixture.destroy();
    await flush();

    expect(emitted).toEqual([]);
  });
});
