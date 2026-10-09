import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, map, of } from 'rxjs';
import { UserSummary } from '../../core/auth/auth.models';
import { CATEGORIES, categoryInfo } from '../../shared/courses/categories';
import { CourseCategory } from '../../shared/courses/course.models';
import { ManagedCourseSummary } from '../trainer/teaching.models';
import { TeachingService } from '../trainer/teaching.service';
import { AdminCoursesService } from './admin-courses.service';
import { AdminUser, AdminUsersService } from './admin-users.service';

/** Trainers of the open panel: loading, loaded, or failed. */
type PanelTrainers = { state: 'loading' } | { state: 'ready'; list: UserSummary[] } | { state: 'error' };

/**
 * « Formations » (Admin) — US-COURSE-03 (create a course), US-ADMIN-07 (assign / unassign the
 * trainers of a course), plus a link to the course editor (shared with the Trainer).
 * Route: /admin/courses.
 *
 * - The list is GET /api/me/managed-courses: for an Admin it returns every course, DRAFT included.
 * - The trainers of a course are loaded when its panel is opened (one call per opened course,
 *   not one per course on page load).
 * - Only ACTIVE Trainer accounts are offered for assignment; the backend refuses a non-Trainer (422).
 */
@Component({
  selector: 'app-admin-courses-page',
  imports: [RouterLink, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './admin-courses-page.html',
  styleUrl: './admin-courses-page.scss',
})
export class AdminCoursesPage {
  private readonly teaching = inject(TeachingService);
  private readonly users = inject(AdminUsersService);
  private readonly admin = inject(AdminCoursesService);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly categories = CATEGORIES;
  protected readonly categoryLabel = (code: CourseCategory) => categoryInfo(code).label;

  protected readonly state = signal<'loading' | 'ready' | 'error'>('loading');
  protected readonly courses = signal<ManagedCourseSummary[]>([]);
  /** Active Trainer accounts; null when they could not be loaded (assignment unavailable). */
  protected readonly trainers = signal<AdminUser[] | null>(null);
  protected readonly notice = signal<string | null>(null);

  protected readonly creating = signal(false);
  protected readonly saving = signal(false);
  protected readonly createError = signal<string | null>(null);
  protected readonly courseForm = this.fb.group({
    title: ['', notBlank],
    category: this.fb.control<CourseCategory>('LANGUAGES'),
    description: '',
  });

  protected readonly openId = signal<number | null>(null);
  protected readonly panel = signal<PanelTrainers>({ state: 'loading' });
  protected readonly panelBusy = signal(false);
  protected readonly panelError = signal<string | null>(null);
  protected readonly selectedTrainerId = signal<number | null>(null);

  /** Active trainers not yet assigned to the open course. */
  protected readonly assignable = computed(() => {
    const p = this.panel();
    const assigned = new Set(p.state === 'ready' ? p.list.map((t) => t.id) : []);
    return (this.trainers() ?? []).filter((t) => !assigned.has(t.id));
  });

  constructor() {
    this.load();
  }

  protected load(): void {
    this.state.set('loading');
    forkJoin({
      courses: this.teaching.getManagedCourses(),
      trainers: this.users.listUsers().pipe(
        map((list): AdminUser[] | null => list.filter((u) => u.role === 'TRAINER' && u.active)),
        catchError(() => of(null)),
      ),
    }).subscribe({
      next: ({ courses, trainers }) => {
        this.courses.set(courses);
        this.trainers.set(trainers);
        this.state.set('ready');
      },
      error: () => this.state.set('error'),
    });
  }

  // ---- Course creation ----

  protected openCreate(): void {
    this.courseForm.reset();
    this.createError.set(null);
    this.notice.set(null);
    this.creating.set(true);
  }

  protected cancelCreate(): void {
    this.creating.set(false);
  }

  protected createCourse(): void {
    if (this.courseForm.invalid) {
      this.courseForm.markAllAsTouched();
      return;
    }
    const { title, category, description } = this.courseForm.getRawValue();
    this.saving.set(true);
    this.createError.set(null);
    this.admin
      .createCourse({ title: title.trim(), category, description: description.trim() || null })
      .subscribe({
        next: (course) => {
          this.saving.set(false);
          this.creating.set(false);
          const summary: ManagedCourseSummary = {
            id: course.id,
            title: course.title,
            description: course.description,
            category: course.category,
            status: course.status,
            enrolledStudents: 0,
            submissionsToCorrect: 0,
          };
          this.courses.update((list) => [...list, summary].sort((a, b) => a.title.localeCompare(b.title, 'fr')));
          this.notice.set(`« ${course.title} » est créée en brouillon. Attribuez-lui un formateur.`);
          this.togglePanel(summary);
        },
        error: (error: unknown) => {
          this.saving.set(false);
          this.createError.set(
            error instanceof HttpErrorResponse && error.status === 400
              ? 'Le titre et le domaine sont obligatoires.'
              : "La formation n'a pas pu être créée. Réessayez.",
          );
        },
      });
  }

  // ---- Trainers of a course ----

  protected togglePanel(course: ManagedCourseSummary): void {
    if (this.openId() === course.id) {
      this.openId.set(null);
      return;
    }
    this.openId.set(course.id);
    this.panelError.set(null);
    this.selectedTrainerId.set(null);
    this.panel.set({ state: 'loading' });
    this.admin.listTrainers(course.id).subscribe({
      next: (list) => this.panel.set({ state: 'ready', list }),
      error: () => this.panel.set({ state: 'error' }),
    });
  }

  protected onSelectTrainer(event: Event): void {
    const value = Number((event.target as HTMLSelectElement).value);
    this.selectedTrainerId.set(value > 0 ? value : null);
  }

  protected assign(course: ManagedCourseSummary): void {
    const trainer = this.assignable().find((t) => t.id === this.selectedTrainerId());
    if (!trainer || this.panelBusy()) return;
    this.panelBusy.set(true);
    this.panelError.set(null);
    this.admin.assignTrainer(course.id, trainer.id).subscribe({
      next: () => {
        this.panelBusy.set(false);
        this.selectedTrainerId.set(null);
        this.updatePanel((list) => [...list, trainer]);
        this.notice.set(`${trainer.firstName} ${trainer.lastName} gère maintenant « ${course.title} ».`);
      },
      error: (error: unknown) => {
        this.panelBusy.set(false);
        this.panelError.set(assignErrorMessage(error));
      },
    });
  }

  protected unassign(course: ManagedCourseSummary, trainer: UserSummary): void {
    if (this.panelBusy()) return;
    this.panelBusy.set(true);
    this.panelError.set(null);
    this.admin.unassignTrainer(course.id, trainer.id).subscribe({
      next: () => {
        this.panelBusy.set(false);
        this.updatePanel((list) => list.filter((t) => t.id !== trainer.id));
        this.notice.set(`${trainer.firstName} ${trainer.lastName} ne gère plus « ${course.title} ».`);
      },
      error: () => {
        this.panelBusy.set(false);
        this.panelError.set(`${trainer.firstName} ${trainer.lastName} n'a pas pu être retiré. Réessayez.`);
      },
    });
  }

  private updatePanel(change: (list: UserSummary[]) => UserSummary[]): void {
    this.panel.update((p) => (p.state === 'ready' ? { state: 'ready', list: change(p.list) } : p));
  }
}

/** Like Validators.required, but « only spaces » is empty too (the backend uses @NotBlank). */
function notBlank(control: AbstractControl<string>): ValidationErrors | null {
  return control.value.trim() === '' ? { required: true } : null;
}

function assignErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 409) return 'Ce formateur est déjà attribué à cette formation.';
    if (error.status === 422) return "Ce compte n'est pas un compte formateur.";
  }
  return "Le formateur n'a pas pu être attribué. Réessayez.";
}
