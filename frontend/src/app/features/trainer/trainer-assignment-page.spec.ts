import { registerLocaleData } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import localeFr from '@angular/common/locales/fr';
import { LOCALE_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { FileDownloadService } from '../../core/api/file-download.service';
import { AssignmentDetail, SubmissionDetail } from '../learning/learning.models';
import { managedAssignment } from './teaching-test-data';
import { EnrolledStudent } from './teaching.models';
import { TeachingService } from './teaching.service';
import { TrainerAssignmentPage } from './trainer-assignment-page';

registerLocaleData(localeFr);

describe('TrainerAssignmentPage', () => {
  const teaching = {
    getManagedAssignments: vi.fn(),
    getAssignment: vi.fn(),
    listSubmissions: vi.fn(),
    getStudents: vi.fn(),
    correct: vi.fn(),
  };
  const download = vi.fn();
  let harness: RouterTestingHarness;

  const assignment: AssignmentDetail = {
    id: 30,
    lessonId: 130,
    title: 'Rédiger un email de relance client',
    description: 'Rédigez un email.',
    dueDate: '2099-10-30T22:59:00Z',
    status: 'PUBLISHED',
    files: [],
  };

  const student = (id: number, firstName: string, lastName: string): EnrolledStudent => ({
    id,
    firstName,
    lastName,
    email: `${firstName.toLowerCase()}@example.com`,
    enrolledAt: '2026-09-01T08:00:00Z',
  });

  const submission = (
    id: number,
    studentId: number,
    studentName: string,
    extra: Partial<SubmissionDetail> = {},
  ): SubmissionDetail => ({
    id,
    assignmentId: 30,
    studentId,
    submittedAt: '2026-10-05T08:30:00Z',
    status: 'SUBMITTED',
    grade: null,
    feedback: null,
    correctedByUserId: null,
    correctedAt: null,
    originalFileName: `${studentName.split(' ')[0].toLowerCase()}.pdf`,
    mimeType: 'application/pdf',
    sizeBytes: 2048,
    studentName,
    correctedByName: null,
    ...extra,
  });

  const CAMILLE = submission(12, 5, 'Camille Martin');
  const LUCAS = submission(13, 6, 'Lucas Petit', {
    status: 'CORRECTED',
    grade: 15,
    feedback: 'Très bien',
    correctedByName: 'Paul Durand',
    correctedAt: '2026-10-06T14:00:00Z',
  });

  async function open() {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'trainer/assignments/:assignmentId', component: TrainerAssignmentPage }]),
        { provide: LOCALE_ID, useValue: 'fr-FR' },
        { provide: TeachingService, useValue: teaching },
        { provide: FileDownloadService, useValue: { download } },
      ],
    });
    harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/trainer/assignments/30', TrainerAssignmentPage);
  }

  const root = () => harness.routeNativeElement!;
  const text = () => (root().textContent ?? '').replace(/\s+/g, ' ');
  const studentButtons = () => Array.from(root().querySelectorAll<HTMLButtonElement>('button.student'));
  const panel = () => root().querySelector('.panel')!;
  const gradeInput = () => panel().querySelector<HTMLInputElement>('input[formcontrolname=grade]')!;
  const feedbackInput = () => panel().querySelector<HTMLTextAreaElement>('textarea')!;
  const submit = () => panel().querySelector<HTMLButtonElement>('button[type=submit]')!;

  function type(element: HTMLInputElement | HTMLTextAreaElement, value: string) {
    element.value = value;
    element.dispatchEvent(new Event('input'));
  }

  function save() {
    panel().querySelector('form')!.dispatchEvent(new Event('submit'));
    harness.detectChanges();
  }

  beforeEach(() => {
    teaching.getManagedAssignments
      .mockReset()
      .mockReturnValue(of([managedAssignment(30, { lessonTitle: 'Relancer un client', enrolledStudents: 3 })]));
    teaching.getAssignment.mockReset().mockReturnValue(of(assignment));
    teaching.listSubmissions.mockReset().mockReturnValue(of([LUCAS, CAMILLE]));
    teaching.getStudents
      .mockReset()
      .mockReturnValue(of([student(5, 'Camille', 'Martin'), student(6, 'Lucas', 'Petit'), student(7, 'Inès', 'Bernard')]));
    teaching.correct.mockReset();
    download.mockReset();
  });

  it('shows the header with breadcrumb, deadline and counters', async () => {
    await open();
    expect(root().querySelector('h1')!.textContent).toContain('Rédiger un email de relance client');
    const crumbs = root().querySelector('.crumbs')!;
    expect(crumbs.textContent).toContain('Anglais professionnel — B1');
    expect(crumbs.textContent).toContain('Relancer un client');
    expect(crumbs.querySelectorAll('a')[1].getAttribute('href')).toBe('/trainer/submissions?course=10');
    expect(text()).toContain('2/3 rendus');
    expect(text()).toContain('1 à corriger');
    expect(text()).toContain('Consigne');
  });

  it('lists to correct first, then corrected, then not submitted', async () => {
    await open();
    const rows = studentButtons().map((b) => b.textContent!.replace(/\s+/g, ' '));
    expect(rows[0]).toContain('Camille Martin');
    expect(rows[0]).toContain('À corriger');
    expect(rows[1]).toContain('Lucas Petit');
    expect(rows[1]).toContain('15/20');
    expect(rows[2]).toContain('Inès Bernard');
    expect(rows[2]).toContain('Non rendu');
  });

  it('selects the first submission to correct', async () => {
    await open();
    expect(studentButtons()[0].getAttribute('aria-pressed')).toBe('true');
    expect(panel().querySelector('h2')!.textContent).toContain('Camille Martin');
    expect(panel().textContent).toContain('camille.pdf');
    expect(submit().textContent).toContain('Enregistrer la correction');
  });

  it('downloads the submitted file', async () => {
    download.mockReturnValue(of(undefined));
    await open();
    panel().querySelector<HTMLButtonElement>('.file button')!.click();
    expect(download).toHaveBeenCalledWith('/api/submissions/12/file', 'camille.pdf');
  });

  it('saves a grade typed with a comma and a comment', async () => {
    await open();
    teaching.correct.mockReturnValue(
      of({ ...CAMILLE, status: 'CORRECTED', grade: 13.5, feedback: 'Bon ton', correctedAt: '2026-10-08T10:00:00Z' }),
    );
    type(gradeInput(), '13,5');
    type(feedbackInput(), '  Bon ton  ');
    save();

    expect(teaching.correct).toHaveBeenCalledWith(12, { grade: 13.5, feedback: 'Bon ton' });
    expect(text()).toContain('Correction enregistrée');
    expect(text()).toContain('0 à corriger');
    expect(studentButtons().find((b) => b.textContent!.includes('Camille'))!.textContent).toContain('13,5/20');
    expect(submit().textContent).toContain('Mettre à jour la correction');
  });

  it('accepts a comment without grade', async () => {
    await open();
    teaching.correct.mockReturnValue(of({ ...CAMILLE, status: 'CORRECTED', feedback: 'À revoir' }));
    type(feedbackInput(), 'À revoir');
    save();
    expect(teaching.correct).toHaveBeenCalledWith(12, { grade: null, feedback: 'À revoir' });
  });

  it('refuses an invalid grade without calling the API', async () => {
    await open();
    type(gradeInput(), '21');
    save();
    expect(text()).toContain('entre 0 et 20');
    expect(teaching.correct).not.toHaveBeenCalled();
  });

  it('refuses an empty correction', async () => {
    await open();
    save();
    expect(text()).toContain('Ajoutez une note ou un commentaire');
    expect(teaching.correct).not.toHaveBeenCalled();
  });

  it('explains a failed save and keeps the typed values', async () => {
    await open();
    teaching.correct.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    type(gradeInput(), '12');
    save();
    expect(text()).toContain("n'a pas pu être enregistrée");
    expect(gradeInput().value).toBe('12');
  });

  it('prefills the form with an existing correction', async () => {
    await open();
    studentButtons()[1].click();
    harness.detectChanges();
    expect(gradeInput().value).toBe('15');
    expect(feedbackInput().value).toBe('Très bien');
    expect(panel().textContent!.replace(/\s+/g, ' ')).toContain('Corrigé par Paul Durand le 6 octobre 2026');
  });

  it('says when a student has not submitted', async () => {
    await open();
    studentButtons()[2].click();
    harness.detectChanges();
    expect(panel().textContent).toContain("Inès Bernard n'a pas encore rendu ce devoir");
    expect(panel().querySelector('form')).toBeNull();
  });

  it('still lists the submissions when the students cannot be loaded', async () => {
    teaching.getStudents.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    await open();
    expect(text()).toContain("La liste des inscrits n'a pas pu être chargée");
    expect(studentButtons()).toHaveLength(2);
  });

  it('refuses an assignment outside the trainer courses', async () => {
    teaching.getManagedAssignments.mockReturnValue(of([]));
    await open();
    expect(text()).toContain('ne fait pas partie de vos formations');
  });

  it('explains a 404', async () => {
    teaching.getAssignment.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
    await open();
    expect(text()).toContain("Ce devoir n'existe pas.");
  });
});
