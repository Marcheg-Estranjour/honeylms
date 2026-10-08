import { registerLocaleData } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import localeFr from '@angular/common/locales/fr';
import { LOCALE_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { FileDownloadService } from '../../core/api/file-download.service';
import { CatalogService } from '../catalog/catalog.service';
import { AssignmentPage } from './assignment-page';
import { AssignmentDetail, SubmissionDetail } from './learning.models';
import { LearningService } from './learning.service';
import { SubmissionService } from './submission.service';

registerLocaleData(localeFr);

describe('AssignmentPage', () => {
  const api = { getAssignment: vi.fn(), getMySubmission: vi.fn(), submit: vi.fn(), replace: vi.fn() };
  const getLesson = vi.fn();
  const getCourse = vi.fn();
  const download = vi.fn();
  let harness: RouterTestingHarness;

  const FUTURE = '2099-10-30T22:59:00Z';
  const PAST = '2020-01-01T10:00:00Z';

  const assignment = (dueDate: string | null = FUTURE): AssignmentDetail => ({
    id: 30,
    lessonId: 101,
    title: 'Rédiger un email de relance client',
    description: 'Rédigez un email.\nTon professionnel.',
    dueDate,
    status: 'PUBLISHED',
    files: [{ storedFileId: 5, originalName: 'modele.docx', mimeType: 'x', sizeBytes: 2048 }],
  });

  const submitted: SubmissionDetail = {
    id: 12,
    assignmentId: 30,
    studentId: 4,
    submittedAt: '2026-10-05T08:30:00Z',
    status: 'SUBMITTED',
    grade: null,
    feedback: null,
    correctedByUserId: null,
    correctedAt: null,
    originalFileName: 'relance.pdf',
    mimeType: 'application/pdf',
    sizeBytes: 1536,
  };
  const corrected: SubmissionDetail = {
    ...submitted,
    status: 'CORRECTED',
    grade: 13.5,
    feedback: 'Bon ton.\nAttention aux formules.',
    correctedByUserId: 2,
    correctedAt: '2026-10-06T14:00:00Z',
  };

  async function open() {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'courses/:courseId/assignments/:assignmentId', component: AssignmentPage }]),
        { provide: LOCALE_ID, useValue: 'fr-FR' },
        { provide: SubmissionService, useValue: api },
        { provide: LearningService, useValue: { getLesson } },
        { provide: CatalogService, useValue: { getCourse } },
        { provide: FileDownloadService, useValue: { download } },
      ],
    });
    harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/courses/1/assignments/30', AssignmentPage);
  }

  const root = () => harness.routeNativeElement!;
  const text = () => root().textContent ?? '';
  const tag = () => root().querySelector('.hg-tag');
  const buttons = () => Array.from(root().querySelectorAll<HTMLButtonElement>('button'));
  const button = (label: string) => buttons().find((b) => b.textContent!.includes(label));
  const pdf = (name = 'v2.pdf') => new File(['hello'], name, { type: 'application/pdf' });

  /** Simulates the choice of a file in the hidden <input type="file">. */
  function choose(file: File) {
    const input = root().querySelector<HTMLInputElement>('input[type=file]')!;
    Object.defineProperty(input, 'files', { value: { item: () => file }, configurable: true });
    input.dispatchEvent(new Event('change'));
    harness.detectChanges();
  }

  beforeEach(() => {
    api.getAssignment.mockReset().mockReturnValue(of(assignment()));
    api.getMySubmission.mockReset().mockReturnValue(of(null));
    api.submit.mockReset();
    api.replace.mockReset();
    getLesson.mockReset().mockReturnValue(of({ id: 101, title: 'Relancer un client' }));
    getCourse.mockReset().mockReturnValue(of({ id: 1, title: 'Anglais professionnel — B1' }));
    download.mockReset();
  });

  it('shows the breadcrumb, the instructions as plain text and the attached files', async () => {
    await open();
    const crumbs = root().querySelector('.crumbs')!;
    expect(crumbs.textContent).toContain('Anglais professionnel — B1');
    expect(crumbs.textContent).toContain('Relancer un client');
    expect(crumbs.querySelectorAll('a')[1].getAttribute('href')).toBe('/courses/1/lessons/101');
    expect(root().querySelector('h1')!.textContent).toContain('Rédiger un email de relance client');
    expect(text()).toContain('Date limite :');
    expect(root().querySelector('.text')!.textContent).toContain('Rédigez un email.');
    expect(text()).toContain('modele.docx');
    expect(text()).toContain('2 Ko');
  });

  it('keeps the page usable when the course and lesson titles cannot be loaded', async () => {
    getCourse.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    getLesson.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    await open();
    expect(root().querySelector('h1')!.textContent).toContain('Rédiger');
    expect(root().querySelector('.crumbs')!.textContent).toContain('Leçon');
  });

  it('explains a 403 on the assignment', async () => {
    api.getAssignment.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 403 })));
    await open();
    expect(text()).toContain("Ce devoir n'est pas accessible");
  });

  it('shows « À rendre » and the drop zone when nothing is submitted', async () => {
    await open();
    expect(tag()!.textContent).toContain('À rendre');
    expect(tag()!.classList).toContain('hg-tag');
    expect(text()).toContain('Déposer votre travail');
    expect(text()).toContain('PDF, DOCX, XLSX, PPTX, JPG, PNG, MP3, MP4 · 50 Mo maximum');
    expect(text()).toContain('La correction apparaîtra ici');
  });

  it('refuses a file with a wrong format before sending anything', async () => {
    await open();
    choose(new File(['x'], 'virus.exe'));
    expect(text()).toContain('Format non accepté');
    expect(button('Déposer ce fichier')).toBeUndefined();
  });

  it('asks for a confirmation, then submits the file', async () => {
    await open();
    choose(pdf('relance.pdf'));
    expect(text()).toContain('Fichier sélectionné : relance.pdf');
    expect(api.submit).not.toHaveBeenCalled();

    api.submit.mockReturnValue(of(submitted));
    button('Déposer ce fichier')!.click();
    harness.detectChanges();

    expect(api.submit).toHaveBeenCalledWith(30, expect.any(File));
    expect(text()).toContain('Votre devoir a bien été déposé.');
    expect(tag()!.textContent).toContain('Rendu');
    expect(text()).toContain('Déposé le');
    expect(text()).toContain('En attente de correction.');
  });

  it('cancels a selected file', async () => {
    await open();
    choose(pdf());
    button('Annuler')!.click();
    harness.detectChanges();
    expect(text()).not.toContain('Fichier sélectionné');
    expect(text()).toContain('Déposer votre travail');
  });

  it('explains a submission refused because of the deadline (403)', async () => {
    await open();
    choose(pdf());
    api.submit.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 403 })));
    button('Déposer ce fichier')!.click();
    harness.detectChanges();
    expect(text()).toContain('la date limite est dépassée');
    expect(button('Déposer ce fichier')).toBeDefined();
  });

  it('reloads the existing submission after a 409', async () => {
    await open();
    choose(pdf());
    api.submit.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 409 })));
    api.getMySubmission.mockReturnValue(of(submitted));
    button('Déposer ce fichier')!.click();
    harness.detectChanges();
    expect(text()).toContain('Un dépôt existe déjà');
    expect(text()).toContain('relance.pdf');
    expect(tag()!.textContent).toContain('Rendu');
  });

  it('replaces a submitted file and warns that it resets the correction', async () => {
    api.getMySubmission.mockReturnValue(of(submitted));
    await open();
    expect(text()).toContain('Remplacer le fichier réinitialise la correction');

    choose(pdf('v2.pdf'));
    api.replace.mockReturnValue(of({ ...submitted, originalFileName: 'v2.pdf' }));
    button('Remplacer mon fichier')!.click();
    harness.detectChanges();

    expect(api.replace).toHaveBeenCalledWith(12, expect.any(File));
    expect(text()).toContain('Votre fichier a bien été remplacé.');
    expect(text()).toContain('v2.pdf');
  });

  it('shows the correction with grade, comment and date', async () => {
    api.getMySubmission.mockReturnValue(of(corrected));
    await open();
    expect(tag()!.textContent).toContain('Corrigé');
    const panel = root().querySelector('.correction')!;
    expect(panel.textContent).toContain('13,5');
    expect(panel.textContent).toContain('/ 20');
    expect(panel.textContent).toContain('Attention aux formules.');
    expect(panel.textContent).toContain('Corrigé le 6 octobre 2026');
  });

  it('asks for an explicit confirmation before replacing a corrected submission', async () => {
    api.getMySubmission.mockReturnValue(of(corrected));
    await open();
    choose(pdf());
    expect(text()).toContain('supprimera la note et le commentaire');
    expect(button('Remplacer et réinitialiser la correction')).toBeDefined();
  });

  it('closes the upload after the deadline', async () => {
    api.getAssignment.mockReturnValue(of(assignment(PAST)));
    await open();
    expect(tag()!.textContent).toContain('Non rendu');
    expect(text()).toContain("le dépôt n'est plus possible");
    expect(root().querySelector('input[type=file]')).toBeNull();
  });

  it('keeps the submitted file downloadable after the deadline', async () => {
    api.getAssignment.mockReturnValue(of(assignment(PAST)));
    api.getMySubmission.mockReturnValue(of(submitted));
    download.mockReturnValue(of(undefined));
    await open();
    expect(root().querySelector('input[type=file]')).toBeNull();
    root().querySelector<HTMLButtonElement>('.file.current button')!.click();
    expect(download).toHaveBeenCalledWith('/api/submissions/12/file', 'relance.pdf');
  });

  it('downloads an attached file through the authenticated service', async () => {
    download.mockReturnValue(of(undefined));
    await open();
    root().querySelector<HTMLButtonElement>('.files button')!.click();
    expect(download).toHaveBeenCalledWith('/api/assignments/30/files/5', 'modele.docx');
  });

  it('blocks the upload when the submission state is unknown', async () => {
    api.getMySubmission.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    await open();
    expect(text()).toContain("Votre dépôt n'a pas pu être chargé");
    expect(root().querySelector('input[type=file]')).toBeNull();
    expect(tag()).toBeNull();
  });
});
