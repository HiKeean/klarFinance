import { hlm } from './hlm';

describe('hlm (class merge utility)', () => {
  it('[positive] joins multiple class strings', () => {
    expect(hlm('a', 'b')).toBe('a b');
  });

  it('[positive] merges Tailwind classes so a later conflicting utility wins (tailwind-merge)', () => {
    expect(hlm('p-2', 'p-4')).toBe('p-4');
  });

  it('[positive] supports conditional object syntax via clsx', () => {
    expect(hlm({ 'text-red-500': true, 'text-blue-500': false })).toBe('text-red-500');
  });

  it('[positive] flattens arrays of class values', () => {
    expect(hlm(['a', 'b'], 'c')).toBe('a b c');
  });

  it('[negative] ignores falsy inputs (undefined, null, false, empty string)', () => {
    expect(hlm('a', undefined, null, false as any, '', 'b')).toBe('a b');
  });

  it('[negative] returns an empty string when given no meaningful input', () => {
    expect(hlm()).toBe('');
    expect(hlm(undefined, null, false as any)).toBe('');
  });
});
