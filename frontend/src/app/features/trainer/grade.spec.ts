import { gradeToText, parseGrade } from './grade';

describe('grade parsing', () => {
  it('accepts an empty grade (optional)', () => {
    expect(parseGrade('  ')).toEqual({ ok: true, value: null });
  });

  it('accepts integers and decimals with a comma or a dot', () => {
    expect(parseGrade('0')).toEqual({ ok: true, value: 0 });
    expect(parseGrade('13,5')).toEqual({ ok: true, value: 13.5 });
    expect(parseGrade(' 12.25 ')).toEqual({ ok: true, value: 12.25 });
    expect(parseGrade('20')).toEqual({ ok: true, value: 20 });
  });

  it('rejects out of range, too many decimals and non numbers', () => {
    for (const text of ['20,5', '21', '-1', '12,345', 'abc', '1e1', '12,']) {
      expect(parseGrade(text)).toEqual({ ok: false });
    }
  });

  it('formats a grade for the input field', () => {
    expect(gradeToText(13.5)).toBe('13,5');
    expect(gradeToText(null)).toBe('');
  });
});
