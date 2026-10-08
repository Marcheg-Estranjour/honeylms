/**
 * Parses the grade typed by a trainer. Accepts a comma or a dot (« 13,5 », « 13.5 »).
 * Empty → null (the grade is optional). Rules mirror the backend: 0 ≤ grade ≤ 20,
 * at most 2 decimals (column DECIMAL(4,2)).
 */
export type GradeParse = { ok: true; value: number | null } | { ok: false };

const GRADE_PATTERN = /^\d{1,2}([.,]\d{1,2})?$/;

export function parseGrade(text: string): GradeParse {
  const trimmed = text.trim();
  if (trimmed === '') return { ok: true, value: null };
  if (!GRADE_PATTERN.test(trimmed)) return { ok: false };
  const value = Number(trimmed.replace(',', '.'));
  return value >= 0 && value <= 20 ? { ok: true, value } : { ok: false };
}

/** 13.5 → « 13,5 » for the input field; null → empty. */
export function gradeToText(grade: number | null): string {
  return grade === null ? '' : String(grade).replace('.', ',');
}
