import { NgTemplateOutlet } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Observable, Subscription } from 'rxjs';
import { GENERIC_ERROR_MESSAGE } from '../../core/api/api-error.model';
import { CATEGORIES } from '../../shared/courses/categories';
import { CourseCategory, CourseDetail } from '../../shared/courses/course.models';
import { CourseOutline, CourseOutlineService, OutlineModule } from '../learning/course-outline.service';
import { LessonDetail, ModuleDetail } from '../learning/learning.models';
import { CourseEditorService } from './course-editor.service';

/**
 * « Gérer la formation » (Trainer) — US-COURSE-04/05/06 (edit, publish, unpublish the course),
 * US-MODULE-01/02/03 and US-LESSON-01/03 (create, rename, publish modules and lessons).
 * Route: /trainer/courses/:courseId. The lesson content and its resources are edited on the
 * lesson editor (S9-7b).
 *
 * - The outline comes from CourseOutlineService: for a Trainer the backend returns DRAFT modules
 *   and lessons too, so the same service shows everything here.
 * - After each action the outline is patched locally with the backend answer (no full reload).
 * - Business rule shown to the trainer: a student only sees a lesson when the course, its module
 *   and the lesson are all PUBLISHED (Dossier §5).
 * - Not offered: unpublish a module or a lesson, delete, reorder — the API does not provide them
 *   (logged as tech debt).
 */
@Component({
  selector: 'app-course-editor-page',
  imports: [RouterLink, NgTemplateOutlet, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './course-editor-page.html',
  styleUrl: './course-editor-page.scss',
})
export class CourseEditorPage {
  private readonly outlines = inject(CourseOutlineService);
  private readonly editor = inject(CourseEditorService);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly categories = CATEGORIES;
  protected readonly state = signal<'loading' | 'ready' | 'error'>('loading');
  protected readonly errorMessage = signal('');
  protected readonly outline = signal<CourseOutline | null>(null);

  /** Key of the action in progress (« course », « publish », « m-3 », « l-8 »…), null when idle. */
  protected readonly busy = signal<string | null>(null);
  protected readonly courseNotice = signal<string | null>(null);
  protected readonly courseError = signal<string | null>(null);
  protected readonly contentError = signal<string | null>(null);

  /** Inline forms: module being renamed, module receiving a new lesson, new module form open. */
  protected readonly renamingModuleId = signal<number | null>(null);
  protected readonly addingLessonTo = signal<number | null>(null);
  protected readonly addingModule = signal(false);

  protected readonly courseForm = this.fb.group({
    title: ['', notBlank],
    description: '',
    category: this.fb.control<CourseCategory>('LANGUAGES'),
  });
  protected readonly moduleForm = this.fb.group({ title: ['', notBlank], description: '' });
  protected readonly lessonForm = this.fb.group({ title: ['', notBlank] });

  protected readonly course = computed(() => this.outline()?.course ?? null);
  protected readonly published = computed(() => this.course()?.status === 'PUBLISHED');

  private courseId = 0;
  private loading?: Subscription;

  constructor() {
    inject(ActivatedRoute)
      .paramMap.pipe(takeUntilDestroyed())
      .subscribe((params) => {
        this.courseId = Number(params.get('courseId'));
        this.load();
      });
    inject(DestroyRef).onDestroy(() => this.loading?.unsubscribe());
  }

  protected load(): void {
    this.loading?.unsubscribe();
    this.state.set('loading');
    this.loading = this.outlines.load(this.courseId).subscribe({
      next: (outline) => {
        this.outline.set(outline);
        this.resetCourseForm(outline.course);
        this.state.set('ready');
      },
      error: (error: unknown) => {
        this.errorMessage.set(loadErrorMessage(error));
        this.state.set('error');
      },
    });
  }

  // ---- Course ----

  protected saveCourse(): void {
    if (this.courseForm.invalid) {
      this.courseForm.markAllAsTouched();
      return;
    }
    const { title, description, category } = this.courseForm.getRawValue();
    this.run('course', this.editor.updateCourse(this.courseId, { title: title.trim(), description: blankToNull(description), category }), (course) => {
      this.setCourse(course);
      this.courseNotice.set('Informations enregistrées.');
    });
  }

  protected togglePublication(): void {
    const request = this.published()
      ? this.editor.unpublishCourse(this.courseId)
      : this.editor.publishCourse(this.courseId);
    this.run('publish', request, (course) => {
      this.setCourse(course);
      this.courseNotice.set(
        course.status === 'PUBLISHED'
          ? 'Formation publiée : elle apparaît dans le catalogue.'
          : 'Formation dépubliée : les étudiants inscrits n’y ont plus accès.',
      );
    });
  }

  // ---- Modules ----

  protected openNewModule(): void {
    this.closeInlineForms();
    this.moduleForm.reset();
    this.addingModule.set(true);
  }

  protected openRename(entry: OutlineModule): void {
    this.closeInlineForms();
    this.moduleForm.reset({ title: entry.module.title, description: entry.module.description ?? '' });
    this.renamingModuleId.set(entry.module.id);
  }

  protected saveModule(): void {
    if (this.moduleForm.invalid) {
      this.moduleForm.markAllAsTouched();
      return;
    }
    const body = { title: this.moduleForm.controls.title.value.trim(), description: blankToNull(this.moduleForm.controls.description.value) };
    const moduleId = this.renamingModuleId();
    if (moduleId === null) {
      this.run('new-module', this.editor.createModule(this.courseId, body), (module) => {
        this.patchModules((modules) => [...modules, { module, lessons: [] }]);
        this.closeInlineForms();
      });
    } else {
      this.run(`m-${moduleId}`, this.editor.updateModule(moduleId, body), (module) => {
        this.replaceModule(module);
        this.closeInlineForms();
      });
    }
  }

  protected publishModule(module: ModuleDetail): void {
    this.run(`m-${module.id}`, this.editor.publishModule(module.id), (saved) => this.replaceModule(saved));
  }

  // ---- Lessons ----

  protected openNewLesson(moduleId: number): void {
    this.closeInlineForms();
    this.lessonForm.reset();
    this.addingLessonTo.set(moduleId);
  }

  protected saveLesson(moduleId: number): void {
    if (this.lessonForm.invalid) {
      this.lessonForm.markAllAsTouched();
      return;
    }
    const body = { title: this.lessonForm.controls.title.value.trim(), description: null, content: null };
    this.run('new-lesson', this.editor.createLesson(moduleId, body), (lesson) => {
      this.patchModules((modules) =>
        modules.map((m) => (m.module.id === moduleId ? { ...m, lessons: [...m.lessons, lesson] } : m)),
      );
      this.closeInlineForms();
    });
  }

  protected publishLesson(lesson: LessonDetail): void {
    this.run(`l-${lesson.id}`, this.editor.publishLesson(lesson.id), (saved) =>
      this.patchModules((modules) =>
        modules.map((m) => ({ ...m, lessons: m.lessons.map((l) => (l.id === saved.id ? saved : l)) })),
      ),
    );
  }

  protected closeInlineForms(): void {
    this.addingModule.set(false);
    this.renamingModuleId.set(null);
    this.addingLessonTo.set(null);
    this.contentError.set(null);
  }

  // ---- helpers ----

  /** Runs one action: busy flag, error message in the right block, success callback. */
  private run<T>(key: string, request: Observable<T>, onSuccess: (value: T) => void): void {
    if (this.busy()) return;
    const isCourse = key === 'course' || key === 'publish';
    this.busy.set(key);
    this.courseNotice.set(null);
    (isCourse ? this.courseError : this.contentError).set(null);
    request.subscribe({
      next: (value) => {
        this.busy.set(null);
        onSuccess(value);
      },
      error: (error: unknown) => {
        this.busy.set(null);
        (isCourse ? this.courseError : this.contentError).set(actionErrorMessage(error));
      },
    });
  }

  private setCourse(course: CourseDetail): void {
    this.outline.update((o) => (o ? { ...o, course } : o));
    this.resetCourseForm(course);
  }

  private resetCourseForm(course: CourseDetail): void {
    this.courseForm.reset({ title: course.title, description: course.description ?? '', category: course.category });
  }

  private replaceModule(module: ModuleDetail): void {
    this.patchModules((modules) => modules.map((m) => (m.module.id === module.id ? { ...m, module } : m)));
  }

  private patchModules(change: (modules: OutlineModule[]) => OutlineModule[]): void {
    this.outline.update((o) => {
      if (!o) return o;
      const modules = change(o.modules);
      return { ...o, modules, lessons: modules.flatMap((m) => m.lessons) };
    });
  }
}

/** Like Validators.required, but « only spaces » is empty too (the backend uses @NotBlank). */
function notBlank(control: AbstractControl<string>): ValidationErrors | null {
  return control.value.trim() === '' ? { required: true } : null;
}

function blankToNull(value: string): string | null {
  const trimmed = value.trim();
  return trimmed === '' ? null : trimmed;
}

function loadErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 403) return "Cette formation ne vous est pas attribuée.";
    if (error.status === 404) return "Cette formation n'existe pas.";
  }
  return GENERIC_ERROR_MESSAGE;
}

function actionErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 400) return 'Le titre est obligatoire.';
    if (error.status === 403) return "Vous n'avez pas le droit de modifier cette formation.";
    if (error.status === 404) return "Cet élément n'existe plus. Rechargez la page.";
  }
  return "La modification n'a pas pu être enregistrée. Réessayez.";
}
