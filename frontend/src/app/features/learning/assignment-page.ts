import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, distinctUntilChanged, forkJoin, map, of, switchMap, tap } from 'rxjs';
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
import { AssignmentDetail, AttachedFileSummary, SubmissionDetail } from './learning.models';
import { LearningService } from './learning.service';
import { SubmissionService } from './submission.service';

type Loadable<T> = { state: 'loading' } | { state: 'ready'; value: T } | { state: 'error'; message: string };

/** Header of the page. Titles are null when they could not be loaded (breadcrumb only). */
interface AssignmentView {
  assignment: AssignmentDetail;
  courseTitle: string | null;
  lessonTitle: string | null;
}

/** The student's submission: null = nothing submitted yet; 'error' = could not be loaded. */
type MySubmission = SubmissionDetail | null | 'error';

interface StatusTag {
  label: string;
  tone: 'info' | 'warning' | 'danger' | 'success';
}

/**
 * US-ASSIGN-04 (read an assignment) + US-SUB-01 (submit) + US-SUB-02 (replace until the deadline)
 * + US-SUB-03 (see the correction). Route: /courses/:courseId/assignments/:assignmentId — wireframe « Devoir ».
 *
 * - Choosing a file never sends it: the student confirms first (two steps). When the submission
 *   is already corrected, the confirmation says that the grade and the comment will be lost.
 *   CHOIX TECHNIQUE : inline confirmation instead of window.confirm (accessible, testable, styled).
 * - The deadline is checked here for display only (hide the upload zone); the backend refuses
 *   any upload after the deadline (403) and stays the authority.
 * - « Corrigé par [formateur] le … » uses SubmissionDetail.correctedByName (gap G10, S9-5).
 */
@Component({
  selector: 'app-assignment-page',
  imports: [RouterLink, DatePipe, DecimalPipe, MatButtonModule, FileSizePipe],
  templateUrl: './assignment-page.html',
  styleUrl: './assignment-page.scss',
})
export class AssignmentPage {
  private readonly route = inject(ActivatedRoute);
  private readonly submissions = inject(SubmissionService);
  private readonly learning = inject(LearningService);
  private readonly catalog = inject(CatalogService);
  private readonly downloads = inject(FileDownloadService);

  protected readonly accept = ACCEPT_ATTRIBUTE;
  protected readonly formats = ALLOWED_FORMATS_LABEL;
  protected readonly maxSize = formatFileSize(MAX_UPLOAD_BYTES);
  protected readonly fileTypeLabel = fileTypeLabel;
  protected readonly now = Date.now();

  protected readonly courseId = signal(0);
  protected readonly view = signal<Loadable<AssignmentView>>({ state: 'loading' });
  protected readonly mine = signal<MySubmission>(null);

  /** File chosen but not sent yet (waiting for the confirmation). */
  protected readonly pending = signal<File | null>(null);
  protected readonly fileError = signal<string | null>(null);
  protected readonly uploading = signal(false);
  protected readonly uploadError = signal<string | null>(null);
  protected readonly notice = signal<string | null>(null);
  protected readonly dragging = signal(false);
  /** Key of the file being downloaded (« a-<id> » attached file, « mine » own file). */
  protected readonly downloading = signal<string | null>(null);
  protected readonly downloadError = signal<string | null>(null);

  protected readonly assignment = computed(() => {
    const v = this.view();
    return v.state === 'ready' ? v.value.assignment : null;
  });

  /** The submission when there is one (null otherwise, also when it could not be loaded). */
  protected readonly submission = computed(() => {
    const m = this.mine();
    return m === 'error' ? null : m;
  });

  protected readonly pastDue = computed(() => {
    const due = this.assignment()?.dueDate;
    return due != null && Date.parse(due) < this.now;
  });

  /** Upload zone visible: before the deadline and when the submission state is known. */
  protected readonly canUpload = computed(() => !this.pastDue() && this.mine() !== 'error');

  protected readonly status = computed((): StatusTag | null => {
    const m = this.mine();
    if (m === 'error' || !this.assignment()) return null;
    if (m?.status === 'CORRECTED') return { label: 'Corrigé', tone: 'success' };
    if (m) return { label: 'Rendu', tone: 'info' };
    return this.pastDue() ? { label: 'Non rendu', tone: 'danger' } : { label: 'À rendre', tone: 'warning' };
  });

  constructor() {
    this.route.paramMap
      .pipe(
        map((p) => ({ courseId: Number(p.get('courseId')), assignmentId: Number(p.get('assignmentId')) })),
        distinctUntilChanged((a, b) => a.assignmentId === b.assignmentId && a.courseId === b.courseId),
        tap(({ courseId }) => {
          this.courseId.set(courseId);
          this.view.set({ state: 'loading' });
          this.resetUpload();
          this.notice.set(null);
        }),
        switchMap(({ courseId, assignmentId }) =>
          forkJoin({
            assignment: this.submissions.getAssignment(assignmentId),
            mine: this.submissions
              .getMySubmission(assignmentId)
              .pipe(catchError(() => of<MySubmission>('error'))),
            courseTitle: this.catalog.getCourse(courseId).pipe(
              map((c) => c.title),
              catchError(() => of(null)),
            ),
          }).pipe(
            switchMap(({ assignment, mine, courseTitle }) =>
              this.learning.getLesson(assignment.lessonId).pipe(
                map((lesson): string | null => lesson.title),
                catchError(() => of(null)),
                map((lessonTitle) => ({ assignment, mine, courseTitle, lessonTitle })),
              ),
            ),
            catchError((error: unknown) => of({ error: assignmentErrorMessage(error) })),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((result) => {
        if ('error' in result) {
          this.view.set({ state: 'error', message: result.error });
          return;
        }
        const { mine, ...value } = result;
        this.mine.set(mine);
        this.view.set({ state: 'ready', value });
      });
  }

  // ---- Choosing a file (input or drag & drop) ----

  protected onFileInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.choose(input.files?.item(0) ?? null);
    input.value = ''; // choosing the same file again must trigger « change »
  }

  protected onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(true);
  }

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragging.set(false);
    this.choose(event.dataTransfer?.files.item(0) ?? null);
  }

  protected choose(file: File | null): void {
    if (!file || this.uploading()) return;
    this.notice.set(null);
    this.uploadError.set(null);
    const error = checkUploadFile(file);
    this.fileError.set(error);
    this.pending.set(error ? null : file);
  }

  protected cancel(): void {
    this.resetUpload();
  }

  // ---- Sending ----

  /** First submission (POST) or replacement (PUT), after the student confirmed. */
  protected send(): void {
    const file = this.pending();
    const assignment = this.assignment();
    if (!file || !assignment || this.uploading()) return;
    const current = this.submission();

    this.uploading.set(true);
    this.uploadError.set(null);
    const request = current
      ? this.submissions.replace(current.id, file)
      : this.submissions.submit(assignment.id, file);

    request.subscribe({
      next: (saved) => {
        this.mine.set(saved);
        this.resetUpload();
        this.notice.set(current ? 'Votre fichier a bien été remplacé.' : 'Votre devoir a bien été déposé.');
      },
      error: (error: unknown) => {
        this.uploading.set(false);
        this.uploadError.set(uploadErrorMessage(error));
        if (error instanceof HttpErrorResponse && error.status === 409) {
          this.reloadMySubmission(assignment.id);
        }
      },
    });
  }

  // ---- Downloads ----

  protected downloadAttached(file: AttachedFileSummary): void {
    const assignment = this.assignment();
    if (!assignment) return;
    this.startDownload(
      `a-${file.storedFileId}`,
      `/api/assignments/${assignment.id}/files/${file.storedFileId}`,
      file.originalName,
    );
  }

  protected downloadMine(submission: SubmissionDetail): void {
    this.startDownload('mine', `/api/submissions/${submission.id}/file`, submission.originalFileName);
  }

  private startDownload(key: string, url: string, fileName: string): void {
    this.downloading.set(key);
    this.downloadError.set(null);
    this.downloads.download(url, fileName).subscribe({
      next: () => this.downloading.set(null),
      error: () => {
        this.downloading.set(null);
        this.downloadError.set(`« ${fileName} » n'a pas pu être téléchargé. Réessayez.`);
      },
    });
  }

  /** After a 409 (submitted from another tab): show what the backend really has. */
  private reloadMySubmission(assignmentId: number): void {
    this.submissions.getMySubmission(assignmentId).subscribe({
      next: (submission) => {
        this.mine.set(submission);
        this.pending.set(null);
      },
      error: () => this.mine.set('error'),
    });
  }

  private resetUpload(): void {
    this.pending.set(null);
    this.fileError.set(null);
    this.uploading.set(false);
    this.uploadError.set(null);
    this.dragging.set(false);
  }
}

function assignmentErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 403) return "Ce devoir n'est pas accessible. Vérifiez votre inscription au cours.";
    if (error.status === 404) return "Ce devoir n'existe pas.";
  }
  return GENERIC_ERROR_MESSAGE;
}

/** Backend messages are in English: the statuses are mapped to French messages here. */
function uploadErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    switch (error.status) {
      case 403:
        return "Le dépôt n'est plus possible : la date limite est dépassée.";
      case 409:
        return 'Un dépôt existe déjà pour ce devoir. Il est affiché ci-dessus : vous pouvez le remplacer.';
      case 413:
      case 422:
        return `Ce fichier a été refusé. Formats acceptés : ${ALLOWED_FORMATS_LABEL} ; ${formatFileSize(MAX_UPLOAD_BYTES)} maximum.`;
    }
  }
  return "Le fichier n'a pas pu être envoyé. Vérifiez votre connexion et réessayez.";
}
