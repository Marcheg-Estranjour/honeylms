import { Component, input } from '@angular/core';
import { CourseCategory } from './course.models';

/**
 * Domain pictogram drawn inline in SVG (no icon font, no external request).
 * Courses have no image in the data model: the card shows the icon of its domain.
 * Decorative only (aria-hidden): the domain name is always written next to it.
 */
@Component({
  selector: 'app-category-icon',
  template: `
    <svg
      [attr.width]="size()"
      [attr.height]="size()"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="1.5"
      stroke-linecap="round"
      stroke-linejoin="round"
      aria-hidden="true"
    >
      @switch (category()) {
        @case ('LANGUAGES') {
          <circle cx="12" cy="12" r="9" />
          <path d="M3 12h18" />
          <path d="M12 3a14 14 0 0 1 0 18" />
          <path d="M12 3a14 14 0 0 0 0 18" />
        }
        @case ('OFFICE_AUTOMATION') {
          <rect x="3" y="4" width="18" height="16" rx="2" />
          <path d="M3 10h18" />
          <path d="M9 4v16" />
          <path d="M15 4v16" />
        }
        @case ('EDUCTOUR') {
          <path d="M3 20h18" />
          <path d="M12 20c0-6 1-10 4-13" />
          <path d="M16 7c-3-1-6 0-8 2" />
          <path d="M16 7c1-3 4-3 5-2" />
          <path d="M16 7c0 3 2 5 4 5" />
        }
      }
    </svg>
  `,
  styles: `
    :host {
      display: inline-flex;
    }
  `,
})
export class CategoryIcon {
  readonly category = input.required<CourseCategory>();
  readonly size = input(44);
}
