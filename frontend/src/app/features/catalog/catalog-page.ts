import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';
import { CATEGORIES, CategoryInfo } from '../../shared/courses/categories';
import { CategoryIcon } from '../../shared/courses/category-icon';
import { CourseCategory, CourseSummary } from '../../shared/courses/course.models';
import { EnrollmentService } from '../learning/enrollment.service';
import { CatalogService } from './catalog.service';

type LoadState = 'loading' | 'ready' | 'error';

interface CatalogSection {
  category: CategoryInfo;
  courses: CourseSummary[];
}

/**
 * US-COURSE-01 — Published courses grouped by domain (Langues / Bureautique / EDUCTOUR).
 *
 * CHOIX TECHNIQUE : one GET /api/courses, then grouping and filtering in the browser.
 * The catalogue is small (a few dozen courses): no extra request when a filter is clicked.
 * The « Inscrit » badge comes from GET /api/me/courses; if that call fails, the catalogue
 * is still displayed, simply without the badges.
 */
@Component({
  selector: 'app-catalog-page',
  imports: [RouterLink, MatButtonModule, CategoryIcon],
  templateUrl: './catalog-page.html',
  styleUrl: './catalog-page.scss',
})
export class CatalogPage {
  private readonly catalog = inject(CatalogService);
  private readonly enrollments = inject(EnrollmentService);

  protected readonly categories = CATEGORIES;
  protected readonly state = signal<LoadState>('loading');
  protected readonly courses = signal<CourseSummary[]>([]);
  protected readonly enrolledIds = signal<ReadonlySet<number>>(new Set());
  /** null = « Tous ». */
  protected readonly filter = signal<CourseCategory | null>(null);

  protected readonly sections = computed<CatalogSection[]>(() => {
    const selected = this.filter();
    return CATEGORIES.filter((category) => selected === null || category.code === selected)
      .map((category) => ({
        category,
        courses: this.courses().filter((course) => course.category === category.code),
      }))
      .filter((section) => section.courses.length > 0);
  });

  constructor() {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    forkJoin({
      courses: this.catalog.getCourses(),
      mine: this.enrollments.getMyCourses().pipe(catchError(() => of([]))),
    }).subscribe({
      next: ({ courses, mine }) => {
        this.courses.set(courses);
        this.enrolledIds.set(new Set(mine.map((enrolled) => enrolled.courseId)));
        this.state.set('ready');
      },
      error: () => this.state.set('error'),
    });
  }

  protected select(category: CourseCategory | null): void {
    this.filter.set(category);
  }
}
