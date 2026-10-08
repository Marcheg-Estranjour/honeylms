import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';
import { GENERIC_ERROR_MESSAGE } from '../../core/api/api-error.model';
import { CourseOutlineService } from './course-outline.service';
import { LearningService } from './learning.service';

/**
 * Entry point of a course: /courses/:courseId/learn (« Accéder au cours », « Continuer », « Reprendre »).
 * US-PROGRESS-04 — goes to the last lesson viewed if it is still accessible, otherwise to the
 * first lesson of the course. The URL is replaced, so « back » does not loop through this page.
 */
@Component({
  selector: 'app-learn-entry-page',
  imports: [RouterLink, MatButtonModule],
  template: `
    @if (message(); as text) {
      <div class="message" role="alert">
        <p>{{ text }}</p>
        <a mat-stroked-button [routerLink]="['/courses', courseId]">Voir la fiche du cours</a>
      </div>
    } @else {
      <p class="message" role="status">Ouverture du cours…</p>
    }
  `,
  styles: `
    .message {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      gap: 12px;
      color: var(--hg-muted);
    }
    p {
      margin: 0;
    }
  `,
})
export class LearnEntryPage {
  private readonly outlines = inject(CourseOutlineService);
  private readonly learning = inject(LearningService);
  private readonly router = inject(Router);

  protected readonly courseId = Number(inject(ActivatedRoute).snapshot.paramMap.get('courseId'));
  protected readonly message = signal<string | null>(null);

  constructor() {
    forkJoin({
      outline: this.outlines.load(this.courseId),
      resume: this.learning.getResumePoint(this.courseId).pipe(catchError(() => of(null))),
    }).subscribe({
      next: ({ outline, resume }) => {
        const target =
          outline.lessons.find((lesson) => lesson.id === resume?.lessonId) ?? outline.lessons[0];
        if (!target) {
          this.message.set("Aucune leçon n'est encore publiée dans ce cours.");
          return;
        }
        void this.router.navigate(['/courses', this.courseId, 'lessons', target.id], { replaceUrl: true });
      },
      error: (error: unknown) => this.message.set(entryErrorMessage(error)),
    });
  }
}

function entryErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 403) return 'Inscrivez-vous à ce cours pour accéder à ses leçons.';
    if (error.status === 404) return "Ce cours n'existe pas.";
  }
  return GENERIC_ERROR_MESSAGE;
}
