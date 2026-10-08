import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, forkJoin, map, of, Subscription, switchMap } from 'rxjs';
import { GENERIC_ERROR_MESSAGE } from '../../core/api/api-error.model';
import { FileDownloadService } from '../../core/api/file-download.service';
import { FileSizePipe } from '../../shared/format/file-size.pipe';
import { AssignmentDetail, SubmissionDetail } from '../learning/learning.models';
import { gradeToText, parseGrade } from './grade';
import { EnrolledStudent, ManagedAssignmentSummary } from './teaching.models';
import { TeachingService } from './teaching.service';

/** One line of the student list: an enrolled student and/or a submission. */
interface StudentRow {
  key: string;
  name: string;
  email: string | null;
  submission: SubmissionDetail | null;
}

interface PageData {
  summary: ManagedAssignmentSummary;
  assignment: AssignmentDetail;
}

/** Display order: to correct, then corrected, then not submitted. */
const ORDER = { SUBMITTED: 0, CORRECTED: 1, NONE: 2 } as const;

/**
 * « Corriger un devoir » (Trainer) — US-SUB-05 (list the submissions), US-SUB-06 (grade /20,
 * optional) and US-SUB-07 (feedback). Route: /trainer/assignments/:assignmentId.
 *
 * - Data: the assignment summary (course, lesson, counters) comes from /api/me/managed-assignments,
 *   which is also the access check (not in the list = not managed). Then the assignment itself,
 *   its submissions and the enrolled students (gap G10) to show who has NOT submitted.
 * - If the enrolled students cannot be loaded, the submissions are still shown and gradable.
 * - HYPOTHÈSE : a correction needs at least a grade or a comment (the API accepts both empty,
 *   but an empty correction gives the student nothing). To be confirmed with the tutor.
 */
@Component({
  selector: 'app-trainer-assignment-page',
  imports: [
    RouterLink,
    DatePipe,
    DecimalPipe,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    FileSizePipe,
  ],
  templateUrl: './trainer-assignment-page.html',
  styleUrl: './trainer-assignment-page.scss',
})
export class TrainerAssignmentPage {
  private readonly teaching = inject(TeachingService);
  private readonly downloads = inject(FileDownloadService);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  private assignmentId = 0;
  private loading?: Subscription;

  protected readonly state = signal<'loading' | 'ready' | 'error'>('loading');
  protected readonly errorMessage = signal('');
  protected readonly data = signal<PageData | null>(null);
  protected readonly submissions = signal<SubmissionDetail[]>([]);
  /** null = could not be loaded (only the submissions are listed). */
  protected readonly students = signal<EnrolledStudent[] | null>(null);
  protected readonly selectedKey = signal<string | null>(null);

  protected readonly saving = signal(false);
  protected readonly saveError = signal<string | null>(null);
  protected readonly notice = signal<string | null>(null);
  protected readonly downloading = signal(false);
  protected readonly downloadError = signal<string | null>(null);
  protected readonly now = Date.now();

  protected readonly form = this.fb.group({ grade: '', feedback: '' });

  protected readonly rows = computed((): StudentRow[] => {
    const byStudent = new Map(this.submissions().map((s) => [s.studentId, s]));
    const rows: StudentRow[] = [];
    for (const student of this.students() ?? []) {
      rows.push({
        key: `u${student.id}`,
        name: `${student.firstName} ${student.lastName}`.trim(),
        email: student.email,
        submission: byStudent.get(student.id) ?? null,
      });
      byStudent.delete(student.id);
    }
    // Submissions of students not in the list (list unavailable, or no longer enrolled).
    for (const s of byStudent.values()) {
      rows.push({ key: `u${s.studentId}`, name: s.studentName || `Étudiant n°${s.studentId}`, email: null, submission: s });
    }
    return rows.sort(
      (a, b) =>
        ORDER[a.submission?.status ?? 'NONE'] - ORDER[b.submission?.status ?? 'NONE'] ||
        a.name.localeCompare(b.name, 'fr'),
    );
  });

  protected readonly selected = computed(() => this.rows().find((r) => r.key === this.selectedKey()) ?? null);

  protected readonly toCorrect = computed(() => this.submissions().filter((s) => s.status === 'SUBMITTED').length);

  protected readonly pastDue = computed(() => {
    const due = this.data()?.assignment.dueDate;
    return due != null && Date.parse(due) < this.now;
  });

  constructor() {
    inject(ActivatedRoute)
      .paramMap.pipe(takeUntilDestroyed())
      .subscribe((params) => {
        this.assignmentId = Number(params.get('assignmentId'));
        this.load();
      });
    this.destroyRef.onDestroy(() => this.loading?.unsubscribe());
  }

  /** (Re)loads everything; a previous load still running is cancelled. */
  protected load(): void {
    const id = this.assignmentId;
    this.loading?.unsubscribe();
    this.state.set('loading');
    this.selectedKey.set(null);
    this.loading = forkJoin({
      summary: this.teaching.getManagedAssignments().pipe(map((list) => list.find((a) => a.id === id) ?? null)),
      assignment: this.teaching.getAssignment(id),
      submissions: this.teaching.listSubmissions(id),
    })
      .pipe(
        switchMap(({ summary, assignment, submissions }) => {
          if (!summary) throw new HttpErrorResponse({ status: 403 });
          return this.teaching.getStudents(summary.courseId).pipe(
            catchError(() => of(null)),
            map((students) => ({ summary, assignment, submissions, students })),
          );
        }),
      )
      .subscribe({
        next: ({ summary, assignment, submissions, students }) => {
          this.data.set({ summary, assignment });
          this.submissions.set(submissions);
          this.students.set(students);
          this.state.set('ready');
          const first = this.rows().find((r) => r.submission?.status === 'SUBMITTED') ?? this.rows()[0];
          if (first) this.select(first);
        },
        error: (error: unknown) => {
          this.errorMessage.set(pageErrorMessage(error));
          this.state.set('error');
        },
      });
  }

  protected select(row: StudentRow): void {
    this.selectedKey.set(row.key);
    this.saveError.set(null);
    this.notice.set(null);
    this.downloadError.set(null);
    this.form.reset({
      grade: gradeToText(row.submission?.grade ?? null),
      feedback: row.submission?.feedback ?? '',
    });
  }

  protected save(): void {
    const submission = this.selected()?.submission;
    if (!submission || this.saving()) return;

    const grade = parseGrade(this.form.controls.grade.value);
    const feedback = this.form.controls.feedback.value.trim();
    if (!grade.ok) {
      this.saveError.set('La note doit être un nombre entre 0 et 20, avec 2 décimales au plus (ex. : 13,5).');
      return;
    }
    if (grade.value === null && feedback === '') {
      this.saveError.set('Ajoutez une note ou un commentaire avant d’enregistrer.');
      return;
    }

    this.saving.set(true);
    this.saveError.set(null);
    this.notice.set(null);
    this.teaching.correct(submission.id, { grade: grade.value, feedback: feedback || null }).subscribe({
      next: (saved) => {
        this.saving.set(false);
        this.submissions.update((list) => list.map((s) => (s.id === saved.id ? saved : s)));
        this.form.markAsPristine();
        this.notice.set('Correction enregistrée. L’étudiant la voit dès maintenant.');
      },
      error: (error: unknown) => {
        this.saving.set(false);
        this.saveError.set(saveErrorMessage(error));
      },
    });
  }

  protected download(submission: SubmissionDetail, studentName: string): void {
    this.downloading.set(true);
    this.downloadError.set(null);
    this.downloads.download(`/api/submissions/${submission.id}/file`, submission.originalFileName).subscribe({
      next: () => this.downloading.set(false),
      error: () => {
        this.downloading.set(false);
        this.downloadError.set(`Le fichier de ${studentName} n'a pas pu être téléchargé. Réessayez.`);
      },
    });
  }
}

function pageErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 403) return "Ce devoir ne fait pas partie de vos formations.";
    if (error.status === 404) return "Ce devoir n'existe pas.";
  }
  return GENERIC_ERROR_MESSAGE;
}

function saveErrorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 400) return 'La note doit être comprise entre 0 et 20.';
    if (error.status === 403) return "Vous n'avez pas le droit de corriger ce devoir.";
  }
  return "La correction n'a pas pu être enregistrée. Réessayez.";
}
