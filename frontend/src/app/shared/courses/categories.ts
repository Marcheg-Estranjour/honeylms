import { CourseCategory } from './course.models';

export interface CategoryInfo {
  code: CourseCategory;
  /** Label shown in the interface (French). */
  label: string;
  /** Global CSS class giving the domain colors (defined in src/styles.scss). */
  cssClass: 'cat-languages' | 'cat-office' | 'cat-eductour';
}

/** Display order of the domains in the catalogue (mock-up « Catalogue »). */
export const CATEGORIES: readonly CategoryInfo[] = [
  { code: 'LANGUAGES', label: 'Langues', cssClass: 'cat-languages' },
  { code: 'OFFICE_AUTOMATION', label: 'Bureautique', cssClass: 'cat-office' },
  { code: 'EDUCTOUR', label: 'EDUCTOUR', cssClass: 'cat-eductour' },
];

const BY_CODE = new Map(CATEGORIES.map((c) => [c.code, c]));

export function categoryInfo(code: CourseCategory): CategoryInfo {
  // The backend only sends the three known codes (CHECK constraint on course.category).
  return BY_CODE.get(code)!;
}
