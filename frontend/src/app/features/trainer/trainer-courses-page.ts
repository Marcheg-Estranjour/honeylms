import { Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { categoryInfo, CategoryInfo } from '../../shared/courses/categories';
import { CategoryIcon } from '../../shared/courses/category-icon';
import { ManagedCourseSummary } from './teaching.models';
import { TeachingService } from './teaching.service';

interface CourseCard {
  course: ManagedCourseSummary;
  category: CategoryInfo;
}

/**
 * « Mes formations » (Trainer) — courses assigned to the trainer, DRAFT included (gap G5).
 * Each card shows the publication status, the enrolled students and the submissions to correct
 * (link to « Corrections » filtered on the course). Editing a course comes with S9-7.
 */
@Component({
  selector: 'app-trainer-courses-page',
  imports: [RouterLink, MatButtonModule, CategoryIcon],
  templateUrl: './trainer-courses-page.html',
  styleUrl: './trainer-courses-page.scss',
})
export class TrainerCoursesPage {
  private readonly teaching = inject(TeachingService);

  protected readonly state = signal<'loading' | 'ready' | 'error'>('loading');
  protected readonly courses = signal<ManagedCourseSummary[]>([]);

  protected readonly cards = computed((): CourseCard[] =>
    this.courses().map((course) => ({ course, category: categoryInfo(course.category) })),
  );

  protected readonly totalToCorrect = computed(() =>
    this.courses().reduce((sum, c) => sum + c.submissionsToCorrect, 0),
  );

  constructor() {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.teaching.getManagedCourses().subscribe({
      next: (courses) => {
        this.courses.set(courses);
        this.state.set('ready');
      },
      error: () => this.state.set('error'),
    });
  }
}
