import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

/**
 * Temporary page for routes whose screen is planned in a later sprint.
 * Keeps the navigation testable from the first increment; removed screen by screen.
 * Route data: `heading` (page title) and `sprint` (planned sprint).
 */
@Component({
  selector: 'app-coming-soon-page',
  template: `
    <h1>{{ heading }}</h1>
    <p class="note">Cet écran arrive au sprint {{ sprint }}.</p>
  `,
  styles: `
    .note {
      margin-top: 12px;
      color: var(--hg-muted);
    }
  `,
})
export class ComingSoonPage {
  private readonly data = inject(ActivatedRoute).snapshot.data;
  protected readonly heading: string = this.data['heading'] ?? '';
  protected readonly sprint: string = this.data['sprint'] ?? '';
}
