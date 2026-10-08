import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { FileDownloadService } from '../../core/api/file-download.service';
import { CatalogService } from '../catalog/catalog.service';
import { LessonDetail, ResourceDetail } from '../learning/learning.models';
import { LearningService } from '../learning/learning.service';
import { CourseEditorService } from './course-editor.service';
import { LessonEditorPage } from './lesson-editor-page';

describe('LessonEditorPage', () => {
  const learning = { getLesson: vi.fn(), listResources: vi.fn() };
  const getCourse = vi.fn();
  const editor = { updateLesson: vi.fn(), publishLesson: vi.fn(), uploadResource: vi.fn(), deleteResource: vi.fn() };
  const download = vi.fn();
  let harness: RouterTestingHarness;

  const LESSON: LessonDetail = {
    id: 12,
    moduleId: 1,
    title: 'Small talk',
    description: null,
    content: 'Hello!\nHow are you?',
    displayOrder: 2,
    status: 'DRAFT',
  };
  const VOCAB: ResourceDetail = {
    id: 7,
    lessonId: 12,
    title: 'Vocabulaire',
    displayOrder: 1,
    originalFileName: 'vocabulaire.pdf',
    mimeType: 'application/pdf',
    sizeBytes: 1536,
  };

  async function open() {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'trainer/courses/:courseId/lessons/:lessonId', component: LessonEditorPage }]),
        { provide: LearningService, useValue: learning },
        { provide: CatalogService, useValue: { getCourse } },
        { provide: CourseEditorService, useValue: editor },
        { provide: FileDownloadService, useValue: { download } },
      ],
    });
    harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/trainer/courses/10/lessons/12', LessonEditorPage);
  }

  const root = () => harness.routeNativeElement!;
  const text = () => (root().textContent ?? '').replace(/\s+/g, ' ');
  const button = (label: string) =>
    Array.from(root().querySelectorAll<HTMLButtonElement>('button')).find((b) => b.textContent!.includes(label));
  const lessonForm = () => root().querySelector<HTMLFormElement>('form.lesson')!;

  function type(input: HTMLInputElement | HTMLTextAreaElement, value: string) {
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  function choose(file: File) {
    const input = root().querySelector<HTMLInputElement>('input[type=file]')!;
    Object.defineProperty(input, 'files', { value: { item: () => file }, configurable: true });
    input.dispatchEvent(new Event('change'));
    harness.detectChanges();
  }

  function click(label: string) {
    button(label)!.click();
    harness.detectChanges();
  }

  beforeEach(() => {
    learning.getLesson.mockReset().mockReturnValue(of(LESSON));
    learning.listResources.mockReset().mockReturnValue(of([VOCAB]));
    getCourse.mockReset().mockReturnValue(of({ id: 10, title: 'Anglais professionnel — B1' }));
    Object.values(editor).forEach((fn) => fn.mockReset());
    download.mockReset();
  });

  it('shows the breadcrumb, the status and the prefilled form', async () => {
    await open();
    const crumbs = root().querySelector('.crumbs')!;
    expect(crumbs.textContent).toContain('Anglais professionnel — B1');
    expect(crumbs.querySelectorAll('a')[1].getAttribute('href')).toBe('/trainer/courses/10');
    expect(root().querySelector('.head')!.textContent).toContain('Brouillon');
    expect(lessonForm().querySelector('input')!.value).toBe('Small talk');
    expect(lessonForm().querySelector('textarea')!.value).toBe('Hello!\nHow are you?');
    expect(text()).toContain("le HTML n'est pas interprété");
  });

  it('saves the lesson', async () => {
    await open();
    type(lessonForm().querySelector('textarea')!, 'Hello!\n\nNice to meet you.');
    editor.updateLesson.mockReturnValue(of({ ...LESSON, content: 'Hello!\n\nNice to meet you.' }));
    lessonForm().dispatchEvent(new Event('submit'));
    harness.detectChanges();

    expect(editor.updateLesson).toHaveBeenCalledWith(12, {
      title: 'Small talk',
      description: null,
      content: 'Hello!\n\nNice to meet you.',
    });
    expect(text()).toContain('Leçon enregistrée.');
  });

  it('refuses a blank title', async () => {
    await open();
    type(lessonForm().querySelector('input')!, '  ');
    lessonForm().dispatchEvent(new Event('submit'));
    harness.detectChanges();
    expect(editor.updateLesson).not.toHaveBeenCalled();
    expect(text()).toContain('Le titre est obligatoire.');
  });

  it('publishes the lesson', async () => {
    await open();
    editor.publishLesson.mockReturnValue(of({ ...LESSON, status: 'PUBLISHED' }));
    click('Publier la leçon');
    expect(editor.publishLesson).toHaveBeenCalledWith(12);
    expect(root().querySelector('.head')!.textContent).toContain('Publiée');
    expect(button('Publier la leçon')).toBeUndefined();
  });

  it('asks to save unsaved changes before publishing', async () => {
    await open();
    type(lessonForm().querySelector('input')!, 'Small talk 2');
    click('Publier la leçon');
    expect(editor.publishLesson).not.toHaveBeenCalled();
    expect(text()).toContain('Enregistrez vos modifications avant de publier');
  });

  it('lists the resources and downloads one', async () => {
    download.mockReturnValue(of(undefined));
    await open();
    expect(text()).toContain('Vocabulaire');
    expect(text()).toContain('vocabulaire.pdf · 1,5 Ko');
    click('Télécharger');
    expect(download).toHaveBeenCalledWith('/api/resources/7/download', 'vocabulaire.pdf');
  });

  it('uploads a resource with a title prefilled from the file name', async () => {
    await open();
    choose(new File(['x'], 'expressions-utiles.pdf'));
    const title = root().querySelector<HTMLInputElement>('.upload input')!;
    expect(title.value).toBe('expressions-utiles');
    type(title, 'Expressions utiles');

    editor.uploadResource.mockReturnValue(of({ ...VOCAB, id: 8, title: 'Expressions utiles', originalFileName: 'expressions-utiles.pdf' }));
    click('Ajouter la ressource');

    expect(editor.uploadResource).toHaveBeenCalledWith(12, 'Expressions utiles', expect.any(File));
    expect(text()).toContain('« Expressions utiles » a été ajoutée.');
    expect(root().querySelectorAll('.file')).toHaveLength(2);
  });

  it('refuses a file with a wrong format', async () => {
    await open();
    choose(new File(['x'], 'macro.exe'));
    expect(text()).toContain('Format non accepté');
    expect(root().querySelector('.upload')).toBeNull();
  });

  it('explains a file refused by the server', async () => {
    await open();
    choose(new File(['x'], 'cours.pdf'));
    editor.uploadResource.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 422 })));
    click('Ajouter la ressource');
    expect(text()).toContain('Ce fichier a été refusé');
  });

  it('deletes a resource after confirmation', async () => {
    await open();
    click('Supprimer');
    expect(editor.deleteResource).not.toHaveBeenCalled();
    expect(text()).toContain('Supprimer ?');

    editor.deleteResource.mockReturnValue(of(undefined));
    click('Oui, supprimer');
    expect(editor.deleteResource).toHaveBeenCalledWith(7);
    expect(text()).toContain('« Vocabulaire » a été supprimée.');
    expect(text()).toContain('Aucune ressource pour cette leçon.');
  });

  it('can cancel a deletion', async () => {
    await open();
    click('Supprimer');
    click('Non');
    expect(text()).not.toContain('Supprimer ?');
    expect(root().querySelectorAll('.file')).toHaveLength(1);
  });

  it('explains a 403 on the lesson', async () => {
    learning.getLesson.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 403 })));
    await open();
    expect(text()).toContain('ne vous est pas attribuée');
  });
});
