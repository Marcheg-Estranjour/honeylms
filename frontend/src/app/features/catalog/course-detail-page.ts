import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';
import { GENERIC_ERROR_MESSAGE } from '../../core/api/api-error.model';
import { categoryInfo } from '../../shared/courses/categories';
import { CategoryIcon } from '../../shared/courses/category-icon';
import { CourseDetail } from '../../shared/courses/course.models';
import { EnrollmentService } from '../learning/enrollment.service';
import { CatalogService } from './catalog.service';

type LoadState = 'loading' | 'ready' | 'error';

/**
 * US-COURSE-02 (course detail) + US-ENROLL-01 (free, immediate enrollment).
 *
 * The modules are NOT listed here: the backend only exposes them to enrolled students
 * (permitAll limited to /api/courses/*). The program is shown in the course view (S9).
 */
@Component({
  selector: 'app-course-detail-page',
  imports: [RouterLink, DatePipe, MatButtonModule, CategoryIcon],
  templateUrl: './course-detail-page.html',
  styleUrl: './course-detail-page.scss',
})
export class CourseDetailPage {
  private readonly catalog = inject(CatalogService);
  private readonly enrollments = inject(EnrollmentService);
  private readonly courseId = Number(inject(ActivatedRoute).snapshot.paramMap.get('courseId'));

  protected readonly state = signal<LoadState>('loading');
  protected readonly loadError = signal('');
  protected readonly course = signal<CourseDetail | null>(null);
  /** ISO date of the enrollment, or null when the student is not enrolled. */
  protected readonly enrolledAt = signal<string | null>(null);
  protected readonly enrolling = signal(false);
  protected readonly enrollError = signal<string | null>(null);
  protected readonly justEnrolled = signal(false);

  protected readonly category = computed(() => {
    const course = this.course();
    return course ? categoryInfo(course.category) : null;
  });

  constructor() {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    forkJoin({
      course: this.catalog.getCourse(this.courseId),
      mine: this.enrollments.getMyCourses().pipe(catchError(() => of([]))),
    }).subscribe({
      next: ({ course, mine }) => {
        this.course.set(course);
        this.enrolledAt.set(mine.find((e) => e.courseId === course.id)?.enrolledAt ?? null);
        this.state.set('ready');
      },
      error: (error: unknown) => {
        this.loadError.set(loadErrorMessage(error));
        this.state.set('error');
      },
    });
  }

  protected enroll(): void {
    this.enrolling.set(true);
    this.enrollError.set(null);
    this.enrollments.enroll(this.courseId).subscribe({
      next: (response) => {
        this.enrolling.set(false);
        this.enrolledAt.set(response.enrolledAt);
        this.justEnrolled.set(true);
      },
      error: (error: unknown) => {
        this.enrolling.set(false);
        if (error instanceof HttpErrorResponse && error.status === 409) {
          // Already enrolled (other tab, double click...): not an error for the user.
          // Re-read the enrollments to show the real enrollment date.
          this.enrollments.getMyCourses().subscribe({
            next: (mine) =>
              this.enrolledAt.set(
                mine.find((e) => e.courseId === this.courseId)?.enrolledAt ?? new Date().toISOString(),
              ),
            error: () => this.enrolledAt.set(new Date().toISOString()),
          });
          return;
        }
        this.enrollError.set(enrollErrorMessage(error));
      },
    });
  }
}

function loadErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 404) return "Ce cours n'existe pas.";
    if (error.status === 403) return "Ce cours n'est pas disponible pour le moment.";
  }
  return GENERIC_ERROR_MESSAGE;
}

function enrollErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse && error.status === 403) {
    return "Ce cours n'est pas ouvert aux inscriptions.";
  }
  return GENERIC_ERROR_MESSAGE;
}
