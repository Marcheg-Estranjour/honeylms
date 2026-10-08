import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  catchError,
  distinctUntilChanged,
  forkJoin,
  map,
  Observable,
  of,
  switchMap,
  tap,
} from 'rxjs';
import { GENERIC_ERROR_MESSAGE } from '../../core/api/api-error.model';
import { CourseOutline, CourseOutlineService, OutlineModule } from './course-outline.service';
import { CourseProgress, LessonDetail } from './learning.models';
import { LearningService } from './learning.service';

type Loadable<T> = { state: 'loading' } | { state: 'ready'; value: T } | { state: 'error'; message: string };

/**
 * US-LEARNING-07 (read a lesson) + US-PROGRESS-01 (complete it) + US-PROGRESS-02/03 (progress).
 * Route: /courses/:courseId/lessons/:lessonId — mock-up « Leçon ».
 *
 * - The outline (sidebar) is loaded once per course; the lesson part reloads when the
 *   lessonId changes (previous / next) — switchMap cancels a slower previous request.
 * - Opening a lesson records the visit (PUT /view) for the resume point.
 * - CHOIX TECHNIQUE (validé) : the lesson content is displayed as PLAIN TEXT (Angular
 *   interpolation escapes any HTML), line breaks kept with CSS. No HTML is ever interpreted.
 */
@Component({
  selector: 'app-lesson-page',
  imports: [RouterLink, MatButtonModule],
  templateUrl: './lesson-page.html',
  styleUrl: './lesson-page.scss',
})
export class LessonPage {
  private readonly route = inject(ActivatedRoute);
  private readonly outlines = inject(CourseOutlineService);
  private readonly learning = inject(LearningService);

  protected readonly courseId = signal(0);
  protected readonly lessonId = signal(0);
  protected readonly outline = signal<Loadable<CourseOutline>>({ state: 'loading' });
  protected readonly lesson = signal<Loadable<LessonDetail>>({ state: 'loading' });
  protected readonly completedIds = signal<ReadonlySet<number>>(new Set());
  protected readonly progress = signal<CourseProgress | null>(null);
  protected readonly completing = signal(false);
  protected readonly completeError = signal<string | null>(null);

  /** Outline once loaded, or null. */
  protected readonly loadedOutline = computed(() => {
    const o = this.outline();
    return o.state === 'ready' ? o.value : null;
  });

  protected readonly position = computed(() => {
    const outline = this.loadedOutline();
    return outline ? outline.lessons.findIndex((l) => l.id === this.lessonId()) : -1;
  });

  protected readonly previousLesson = computed(() => {
    const outline = this.loadedOutline();
    const i = this.position();
    return outline && i > 0 ? outline.lessons[i - 1] : null;
  });

  protected readonly nextLesson = computed(() => {
    const outline = this.loadedOutline();
    const i = this.position();
    return outline && i >= 0 && i < outline.lessons.length - 1 ? outline.lessons[i + 1] : null;
  });

  protected readonly currentModule = computed(() =>
    this.loadedOutline()?.modules.find((m) => m.lessons.some((l) => l.id === this.lessonId())) ?? null,
  );

  protected readonly isCompleted = computed(() => this.completedIds().has(this.lessonId()));

  constructor() {
    const params = this.route.paramMap.pipe(
      map((p) => ({ courseId: Number(p.get('courseId')), lessonId: Number(p.get('lessonId')) })),
    );

    // Outline + completions + progress: once per course.
    params
      .pipe(
        map((p) => p.courseId),
        distinctUntilChanged(),
        tap((courseId) => {
          this.courseId.set(courseId);
          this.outline.set({ state: 'loading' });
        }),
        switchMap((courseId) => this.loadCourseState(courseId)),
        takeUntilDestroyed(),
      )
      .subscribe();

    // Lesson: every time the lesson changes.
    params
      .pipe(
        map((p) => p.lessonId),
        distinctUntilChanged(),
        tap((lessonId) => {
          this.lessonId.set(lessonId);
          this.lesson.set({ state: 'loading' });
          this.completeError.set(null);
        }),
        switchMap((lessonId) =>
          this.learning.getLesson(lessonId).pipe(
            tap(() => this.recordView(lessonId)),
            map((value): Loadable<LessonDetail> => ({ state: 'ready', value })),
            catchError((error: unknown) => of<Loadable<LessonDetail>>({ state: 'error', message: lessonErrorMessage(error) })),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((result) => this.lesson.set(result));
  }

  /** « 2/4 » for a module of the sidebar. */
  protected moduleCount(entry: OutlineModule): string {
    const done = entry.lessons.filter((l) => this.completedIds().has(l.id)).length;
    return `${done}/${entry.lessons.length}`;
  }

  /** Position of the lesson inside its module (1-based), for the breadcrumb. */
  protected lessonNumber(entry: OutlineModule): number {
    return entry.lessons.findIndex((l) => l.id === this.lessonId()) + 1;
  }

  protected complete(): void {
    const lessonId = this.lessonId();
    this.completing.set(true);
    this.completeError.set(null);
    this.learning
      .markCompleted(lessonId)
      .pipe(switchMap(() => this.learning.getCourseProgress(this.courseId()).pipe(catchError(() => of(null)))))
      .subscribe({
        next: (progress) => {
          this.completing.set(false);
          this.completedIds.update((ids) => new Set(ids).add(lessonId));
          if (progress) this.progress.set(progress);
        },
        error: () => {
          this.completing.set(false);
          this.completeError.set("La leçon n'a pas pu être marquée comme terminée. Réessayez.");
        },
      });
  }

  private loadCourseState(courseId: number): Observable<unknown> {
    return forkJoin({
      outline: this.outlines.load(courseId),
      completions: this.learning.getCompletions(courseId).pipe(catchError(() => of(null))),
      progress: this.learning.getCourseProgress(courseId).pipe(catchError(() => of(null))),
    }).pipe(
      tap(({ outline, completions, progress }) => {
        this.outline.set({ state: 'ready', value: outline });
        this.completedIds.set(new Set(completions?.completedLessonIds ?? []));
        this.progress.set(progress);
      }),
      catchError((error: unknown) => {
        this.outline.set({ state: 'error', message: lessonErrorMessage(error) });
        return of(null);
      }),
    );
  }

  /** Fire and forget: a failed visit record must never block the reading. */
  private recordView(lessonId: number): void {
    this.learning.recordView(lessonId).subscribe({ error: () => undefined });
  }
}

function lessonErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 403) return "Cette leçon n'est pas accessible. Vérifiez votre inscription au cours.";
    if (error.status === 404) return "Cette leçon n'existe pas.";
  }
  return GENERIC_ERROR_MESSAGE;
}
