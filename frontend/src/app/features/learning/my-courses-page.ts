import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, map, Observable, of, switchMap } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { categoryInfo, CategoryInfo } from '../../shared/courses/categories';
import { CategoryIcon } from '../../shared/courses/category-icon';
import { EnrolledCourse } from '../../shared/courses/course.models';
import { EnrollmentService } from './enrollment.service';
import { CourseProgress, ResumePoint } from './learning.models';
import { LearningService } from './learning.service';

type LoadState = 'loading' | 'ready' | 'error';

export interface MyCourseCard {
  course: EnrolledCourse;
  category: CategoryInfo;
  /** null when the progress could not be loaded. */
  progress: CourseProgress | null;
  resume: ResumePoint | null;
}

export interface ResumeBanner {
  card: MyCourseCard;
  /** « Module 2 · Rédiger un email de relance », or null if the lesson could not be read. */
  location: string | null;
}

/**
 * US-ENROLL-02 (my courses) + US-PROGRESS-02 (course progress) + US-PROGRESS-04 (resume).
 *
 * CHOIX TECHNIQUE : the API has no aggregated « dashboard » endpoint, so the page calls
 * /progress and /resume for each enrolled course (2 small calls per course, in parallel).
 * Acceptable for a student's handful of courses; an aggregated endpoint is a COULD HAVE.
 */
@Component({
  selector: 'app-my-courses-page',
  imports: [RouterLink, MatButtonModule, CategoryIcon],
  templateUrl: './my-courses-page.html',
  styleUrl: './my-courses-page.scss',
})
export class MyCoursesPage {
  private readonly enrollments = inject(EnrollmentService);
  private readonly learning = inject(LearningService);
  private readonly auth = inject(AuthService);

  protected readonly firstName = computed(() => this.auth.currentUser()?.firstName ?? '');
  protected readonly state = signal<LoadState>('loading');
  protected readonly cards = signal<MyCourseCard[]>([]);
  protected readonly banner = signal<ResumeBanner | null>(null);

  constructor() {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.enrollments
      .getMyCourses()
      .pipe(
        switchMap((courses) => (courses.length ? forkJoin(courses.map((c) => this.toCard(c))) : of([]))),
        switchMap((cards) => this.buildBanner(cards).pipe(map((banner) => ({ cards, banner })))),
      )
      .subscribe({
        next: ({ cards, banner }) => {
          this.cards.set(cards);
          this.banner.set(banner);
          this.state.set('ready');
        },
        error: () => this.state.set('error'),
      });
  }

  /** Label of the card button, depending on the progress. */
  protected actionLabel(card: MyCourseCard): string {
    if (!card.progress || card.progress.completedLessons === 0) return 'Commencer';
    return card.progress.percentage >= 100 ? 'Revoir le cours' : 'Continuer';
  }

  private toCard(course: EnrolledCourse): Observable<MyCourseCard> {
    return forkJoin({
      progress: this.learning.getCourseProgress(course.courseId).pipe(catchError(() => of(null))),
      resume: this.learning.getResumePoint(course.courseId).pipe(catchError(() => of(null))),
    }).pipe(map(({ progress, resume }) => ({ course, category: categoryInfo(course.category), progress, resume })));
  }

  /** The banner shows the course viewed most recently, if any lesson was ever opened. */
  private buildBanner(cards: MyCourseCard[]): Observable<ResumeBanner | null> {
    const latest = cards
      .filter((card) => card.resume !== null)
      .sort((a, b) => Date.parse(b.resume!.lastViewedAt) - Date.parse(a.resume!.lastViewedAt))[0];
    if (!latest) {
      return of(null);
    }
    const resume = latest.resume!;
    return forkJoin({
      module: this.learning.getModule(resume.moduleId),
      lesson: this.learning.getLesson(resume.lessonId),
    }).pipe(
      map(({ module, lesson }) => ({
        card: latest,
        location: `Module ${module.displayOrder} · ${lesson.title}`,
      })),
      catchError(() => of({ card: latest, location: null })),
    );
  }
}
