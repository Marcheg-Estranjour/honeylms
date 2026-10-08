import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, forkJoin, map, of, Subscription } from 'rxjs';
import { GENERIC_ERROR_MESSAGE } from '../../core/api/api-error.model';
import { FileDownloadService } from '../../core/api/file-download.service';
import { fileTypeLabel, FileSizePipe, formatFileSize } from '../../shared/format/file-size.pipe';
import {
  ACCEPT_ATTRIBUTE,
  ALLOWED_FORMATS_LABEL,
  checkUploadFile,
  MAX_UPLOAD_BYTES,
} from '../../shared/format/upload-rules';
import { CatalogService } from '../catalog/catalog.service';
import { LessonDetail, ResourceDetail } from '../learning/learning.models';
import { LearningService } from '../learning/learning.service';
import { CourseEditorService } from './course-editor.service';

/**
 * « Modifier la leçon » (Trainer) — US-LESSON-02 (edit), US-LESSON-03 (publish),
 * US-FILE-01 (upload a resource), US-FILE-03 (delete a resource).
 * Route: /trainer/courses/:courseId/lessons/:lessonId.
 *
 * - The content is PLAIN TEXT (CHOIX TECHNIQUE validé, S9-2): what the trainer types is shown
 *   as is to the students, line breaks kept, HTML never interpreted.
 * - Upload rules are checked before sending (comfort, shared with the student submission);
 *   the backend re-validates. The title is prefilled with the file name when empty.
 * - Deleting a resource asks for an inline confirmation (it also deletes the stored file).
 */
@Component({
  selector: 'app-lesson-editor-page',
  imports: [RouterLink, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, FileSizePipe],
  templateUrl: './lesson-editor-page.html',
  styleUrl: './lesson-editor-page.scss',
})
export class LessonEditorPage {
  private readonly learning = inject(LearningService);
  private readonly catalog = inject(CatalogService);
  private readonly editor = inject(CourseEditorService);
  private readonly downloads = inject(FileDownloadService);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly accept = ACCEPT_ATTRIBUTE;
  protected readonly formats = ALLOWED_FORMATS_LABEL;
  protected readonly maxSize = formatFileSize(MAX_UPLOAD_BYTES);
  protected readonly fileTypeLabel = fileTypeLabel;

  protected readonly courseId = signal(0);
  protected readonly state = signal<'loading' | 'ready' | 'error'>('loading');
  protected readonly errorMessage = signal('');
  protected readonly lesson = signal<LessonDetail | null>(null);
  protected readonly courseTitle = signal<string | null>(null);
  /** null = could not be loaded. */
  protected readonly resources = signal<ResourceDetail[] | null>(null);

  /** Action in progress: « save », « publish », « upload », « r-7 » (delete / download). */
  protected readonly busy = signal<string | null>(null);
  protected readonly lessonNotice = signal<string | null>(null);
  protected readonly lessonError = signal<string | null>(null);
  protected readonly resourceNotice = signal<string | null>(null);
  protected readonly resourceError = signal<string | null>(null);
  protected readonly confirmDeleteId = signal<number | null>(null);
  protected readonly pendingFile = signal<File | null>(null);

  protected readonly lessonForm = this.fb.group({ title: ['', notBlank], description: '', content: '' });
  protected readonly resourceTitle = this.fb.control('');

  private lessonId = 0;
  private loading?: Subscription;

  constructor() {
    inject(ActivatedRoute)
      .paramMap.pipe(takeUntilDestroyed())
      .subscribe((params) => {
        this.courseId.set(Number(params.get('courseId')));
        this.lessonId = Number(params.get('lessonId'));
        this.load();
      });
    inject(DestroyRef).onDestroy(() => this.loading?.unsubscribe());
  }

  protected load(): void {
    this.loading?.unsubscribe();
    this.state.set('loading');
    this.loading = forkJoin({
      lesson: this.learning.getLesson(this.lessonId),
      resources: this.learning.listResources(this.lessonId).pipe(catchError(() => of(null))),
      courseTitle: this.catalog.getCourse(this.courseId()).pipe(
        map((c): string | null => c.title),
        catchError(() => of(null)),
      ),
    }).subscribe({
      next: ({ lesson, resources, courseTitle }) => {
        this.setLesson(lesson);
        this.resources.set(resources);
        this.courseTitle.set(courseTitle);
        this.state.set('ready');
      },
      error: (error: unknown) => {
        this.errorMessage.set(loadErrorMessage(error));
        this.state.set('error');
      },
    });
  }

  // ---- Lesson ----

  protected save(): void {
    if (this.lessonForm.invalid) {
      this.lessonForm.markAllAsTouched();
      return;
    }
    const { title, description, content } = this.lessonForm.getRawValue();
    const body = { title: title.trim(), description: blankToNull(description), content: blankToNull(content) };
    this.runLesson('save', this.editor.updateLesson(this.lessonId, body), 'Leçon enregistrée.');
  }

  protected publish(): void {
    if (this.lessonForm.dirty) {
      this.lessonError.set('Enregistrez vos modifications avant de publier la leçon.');
      return;
    }
    this.runLesson('publish', this.editor.publishLesson(this.lessonId), 'Leçon publiée.');
  }

  // ---- Resources ----

  protected onFileInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.item(0) ?? null;
    input.value = '';
    if (!file) return;
    this.resourceNotice.set(null);
    const error = checkUploadFile(file);
    this.resourceError.set(error);
    this.pendingFile.set(error ? null : file);
    if (!error && this.resourceTitle.value.trim() === '') {
      this.resourceTitle.setValue(withoutExtension(file.name));
    }
  }

  protected cancelUpload(): void {
    this.pendingFile.set(null);
    this.resourceTitle.reset();
    this.resourceError.set(null);
  }

  protected upload(): void {
    const file = this.pendingFile();
    const title = this.resourceTitle.value.trim();
    if (!file || this.busy()) return;
    if (title === '') {
      this.resourceError.set('Donnez un titre à la ressource.');
      return;
    }
    this.startResourceAction('upload');
    this.editor.uploadResource(this.lessonId, title, file).subscribe({
      next: (resource) => {
        this.busy.set(null);
        this.resources.update((list) => [...(list ?? []), resource]);
        this.pendingFile.set(null);
        this.resourceTitle.reset();
        this.resourceNotice.set(`« ${resource.title} » a été ajoutée.`);
      },
      error: (error: unknown) => {
        this.busy.set(null);
        this.resourceError.set(uploadErrorMessage(error));
      },
    });
  }

  protected askDelete(resource: ResourceDetail): void {
    this.resourceNotice.set(null);
    this.resourceError.set(null);
    this.confirmDeleteId.set(resource.id);
  }

  protected delete(resource: ResourceDetail): void {
    if (this.busy()) return;
    this.startResourceAction(`r-${resource.id}`);
    this.editor.deleteResource(resource.id).subscribe({
      next: () => {
        this.busy.set(null);
        this.confirmDeleteId.set(null);
        this.resources.update((list) => (list ?? []).filter((r) => r.id !== resource.id));
        this.resourceNotice.set(`« ${resource.title} » a été supprimée.`);
      },
      error: () => {
        this.busy.set(null);
        this.confirmDeleteId.set(null);
        this.resourceError.set(`« ${resource.title} » n'a pas pu être supprimée. Réessayez.`);
      },
    });
  }

  protected download(resource: ResourceDetail): void {
    if (this.busy()) return;
    this.startResourceAction(`r-${resource.id}`);
    this.downloads.download(`/api/resources/${resource.id}/download`, resource.originalFileName).subscribe({
      next: () => this.busy.set(null),
      error: () => {
        this.busy.set(null);
        this.resourceError.set(`« ${resource.title} » n'a pas pu être téléchargée. Réessayez.`);
      },
    });
  }

  // ---- helpers ----

  private runLesson(key: string, request: ReturnType<CourseEditorService['updateLesson']>, notice: string): void {
    if (this.busy()) return;
    this.busy.set(key);
    this.lessonNotice.set(null);
    this.lessonError.set(null);
    request.subscribe({
      next: (lesson) => {
        this.busy.set(null);
        this.setLesson(lesson);
        this.lessonNotice.set(notice);
      },
      error: (error: unknown) => {
        this.busy.set(null);
        this.lessonError.set(lessonErrorMessage(error));
      },
    });
  }

  private startResourceAction(key: string): void {
    this.busy.set(key);
    this.resourceNotice.set(null);
    this.resourceError.set(null);
  }

  private setLesson(lesson: LessonDetail): void {
    this.lesson.set(lesson);
    this.lessonForm.reset({
      title: lesson.title,
      description: lesson.description ?? '',
      content: lesson.content ?? '',
    });
  }
}

/** Like Validators.required, but « only spaces » is empty too (the backend uses @NotBlank). */
function notBlank(control: AbstractControl<string>): ValidationErrors | null {
  return control.value.trim() === '' ? { required: true } : null;
}

function blankToNull(value: string): string | null {
  return value.trim() === '' ? null : value;
}

/** « vocabulaire.final.pdf » → « vocabulaire.final ». */
function withoutExtension(fileName: string): string {
  const dot = fileName.lastIndexOf('.');
  return dot > 0 ? fileName.slice(0, dot) : fileName;
}

function loadErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 403) return "Cette leçon appartient à une formation qui ne vous est pas attribuée.";
    if (error.status === 404) return "Cette leçon n'existe pas.";
  }
  return GENERIC_ERROR_MESSAGE;
}

function lessonErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 400) return 'Le titre est obligatoire.';
    if (error.status === 403) return "Vous n'avez pas le droit de modifier cette leçon.";
  }
  return "La leçon n'a pas pu être enregistrée. Réessayez.";
}

function uploadErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 413 || error.status === 422) {
      return `Ce fichier a été refusé. Formats acceptés : ${ALLOWED_FORMATS_LABEL} ; ${formatFileSize(MAX_UPLOAD_BYTES)} maximum.`;
    }
    if (error.status === 403) return "Vous n'avez pas le droit d'ajouter une ressource à cette leçon.";
  }
  return "La ressource n'a pas pu être envoyée. Vérifiez votre connexion et réessayez.";
}
