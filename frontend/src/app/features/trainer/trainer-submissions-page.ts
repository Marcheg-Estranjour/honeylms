import { DatePipe } from '@angular/common';
import { Component, computed, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { ManagedAssignmentSummary } from './teaching.models';
import { TeachingService } from './teaching.service';

interface CourseFilter {
  id: number;
  title: string;
}

/**
 * « Corrections » (Trainer) — assignments of the managed courses with their counters (gap G11):
 * submitted / enrolled, and how many are waiting for a correction. Opening an assignment leads
 * to the grading screen (S9-6b).
 *
 * - The course filter is a query parameter (?course=10), bound through withComponentInputBinding:
 *   « Mes formations » links straight to a filtered list, and the URL can be bookmarked.
 * - The list order comes from the backend (nearest deadline first, no deadline last).
 */
@Component({
  selector: 'app-trainer-submissions-page',
  imports: [RouterLink, DatePipe, MatButtonModule],
  templateUrl: './trainer-submissions-page.html',
  styleUrl: './trainer-submissions-page.scss',
})
export class TrainerSubmissionsPage {
  private readonly teaching = inject(TeachingService);

  /** Query parameter ?course=… (string from the URL). */
  readonly course = input<string>();

  protected readonly state = signal<'loading' | 'ready' | 'error'>('loading');
  protected readonly assignments = signal<ManagedAssignmentSummary[]>([]);
  protected readonly onlyToCorrect = signal(false);
  protected readonly now = Date.now();

  protected readonly courseId = computed(() => {
    const id = Number(this.course());
    return Number.isInteger(id) && id > 0 ? id : null;
  });

  /** Courses having at least one assignment, in order of appearance. */
  protected readonly courses = computed((): CourseFilter[] => {
    const seen = new Map<number, CourseFilter>();
    for (const a of this.assignments()) {
      if (!seen.has(a.courseId)) seen.set(a.courseId, { id: a.courseId, title: a.courseTitle });
    }
    return [...seen.values()].sort((x, y) => x.title.localeCompare(y.title, 'fr'));
  });

  protected readonly visible = computed(() => {
    const courseId = this.courseId();
    return this.assignments().filter(
      (a) => (courseId === null || a.courseId === courseId) && (!this.onlyToCorrect() || a.submissionsToCorrect > 0),
    );
  });

  protected readonly totalToCorrect = computed(() =>
    this.assignments()
      .filter((a) => this.courseId() === null || a.courseId === this.courseId())
      .reduce((sum, a) => sum + a.submissionsToCorrect, 0),
  );

  constructor() {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    this.teaching.getManagedAssignments().subscribe({
      next: (assignments) => {
        this.assignments.set(assignments);
        this.state.set('ready');
      },
      error: () => this.state.set('error'),
    });
  }

  protected isPastDue(a: ManagedAssignmentSummary): boolean {
    return a.dueDate !== null && Date.parse(a.dueDate) < this.now;
  }

  protected toggleOnlyToCorrect(event: Event): void {
    this.onlyToCorrect.set((event.target as HTMLInputElement).checked);
  }
}
