import { describe, expect, it } from 'vitest';
import { format } from './format';

describe('format', () => {
  it('replaces a placeholder with its argument', () => {
    expect(format('a[{index}] = {value}', { index: 2, value: 8 })).toBe('a[2] = 8');
  });

  it('replaces every occurrence of a repeated placeholder', () => {
    expect(format('{value} and {value}', { value: 7 })).toBe('7 and 7');
  });

  it('leaves a placeholder alone when the argument is missing', () => {
    expect(format('{a} {b}', { a: 1 })).toBe('1 {b}');
  });

  it('returns the template when there is nothing to replace', () => {
    expect(format('the array is sorted', {})).toBe('the array is sorted');
  });
});
