import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { CourseDetail } from '../../shared/courses/course.models';
import { CourseOutline, CourseOutlineService } from '../learning/course-outline.service';
import { LessonDetail, ModuleDetail } from '../learning/learning.models';
import { CourseEditorPage } from './course-editor-page';
import { CourseEditorService } from './course-editor.service';

describe('CourseEditorPage', () => {
  const load = vi.fn();
  const editor = {
    updateCourse: vi.fn(),
    publishCourse: vi.fn(),
    unpublishCourse: vi.fn(),
    createModule: vi.fn(),
    updateModule: vi.fn(),
    publishModule: vi.fn(),
    createLesson: vi.fn(),
    publishLesson: vi.fn(),
  };
  let harness: RouterTestingHarness;

  const course: CourseDetail = {
    id: 10,
    title: 'Anglais professionnel — B1',
    description: 'Pour le travail',
    category: 'LANGUAGES',
    status: 'DRAFT',
    createdByUserId: 1,
  };
  const module = (id: number, order: number, title: string, status: 'DRAFT' | 'PUBLISHED'): ModuleDetail => ({
    id,
    courseId: 10,
    title,
    description: null,
    displayOrder: order,
    status,
  });
  const lesson = (id: number, moduleId: number, title: string, status: 'DRAFT' | 'PUBLISHED'): LessonDetail => ({
    id,
    moduleId,
    title,
    description: null,
    content: null,
    displayOrder: 1,
    status,
  });

  const M1 = module(1, 1, 'Se présenter', 'PUBLISHED');
  const M2 = module(2, 2, 'Le téléphone', 'DRAFT');
  const L11 = lesson(11, 1, 'Présentation', 'PUBLISHED');
  const L12 = lesson(12, 1, 'Small talk', 'DRAFT');

  const outline = (): CourseOutline => ({
    course,
    modules: [
      { module: M1, lessons: [L11, L12] },
      { module: M2, lessons: [] },
    ],
    lessons: [L11, L12],
  });

  async function open() {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'trainer/courses/:courseId', component: CourseEditorPage }]),
        { provide: CourseOutlineService, useValue: { load } },
        { provide: CourseEditorService, useValue: editor },
      ],
    });
    harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/trainer/courses/10', CourseEditorPage);
  }

  const root = () => harness.routeNativeElement!;
  const text = () => (root().textContent ?? '').replace(/\s+/g, ' ');
  const button = (label: string, scope: ParentNode = root()) =>
    Array.from(scope.querySelectorAll<HTMLButtonElement>('button')).find((b) => b.textContent!.includes(label));
  const modules = () => Array.from(root().querySelectorAll('.module'));

  function type(input: HTMLInputElement | HTMLTextAreaElement, value: string) {
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  function submit(form: HTMLFormElement) {
    form.dispatchEvent(new Event('submit'));
    harness.detectChanges();
  }

  beforeEach(() => {
    load.mockReset().mockReturnValue(of(outline()));
    Object.values(editor).forEach((fn) => fn.mockReset());
  });

  it('shows the course, its status and the visibility rule', async () => {
    await open();
    expect(root().querySelector('h1')!.textContent).toContain('Anglais professionnel — B1');
    expect(root().querySelector('.head .hg-tag')!.textContent).toContain('Brouillon');
    expect(text()).toContain('la formation, son module et la leçon sont tous publiés');
    expect(load).toHaveBeenCalledWith(10);
  });

  it('prefills the information form', async () => {
    await open();
    const form = root().querySelector<HTMLFormElement>('form.info')!;
    expect(form.querySelector<HTMLInputElement>('input')!.value).toBe('Anglais professionnel — B1');
    expect(form.querySelector<HTMLSelectElement>('select')!.value).toBe('LANGUAGES');
    expect(form.querySelector<HTMLTextAreaElement>('textarea')!.value).toBe('Pour le travail');
  });

  it('saves the information', async () => {
    await open();
    const form = root().querySelector<HTMLFormElement>('form.info')!;
    type(form.querySelector('input')!, '  Anglais B1  ');
    const select = form.querySelector('select')!;
    select.value = 'EDUCTOUR';
    select.dispatchEvent(new Event('change'));
    editor.updateCourse.mockReturnValue(of({ ...course, title: 'Anglais B1', category: 'EDUCTOUR' }));
    submit(form);

    expect(editor.updateCourse).toHaveBeenCalledWith(10, {
      title: 'Anglais B1',
      description: 'Pour le travail',
      category: 'EDUCTOUR',
    });
    expect(text()).toContain('Informations enregistrées.');
    expect(root().querySelector('h1')!.textContent).toContain('Anglais B1');
  });

  it('refuses a blank title without calling the API', async () => {
    await open();
    const form = root().querySelector<HTMLFormElement>('form.info')!;
    type(form.querySelector('input')!, '   ');
    submit(form);
    expect(editor.updateCourse).not.toHaveBeenCalled();
    expect(text()).toContain('Le titre est obligatoire.');
  });

  it('publishes then unpublishes the course', async () => {
    await open();
    editor.publishCourse.mockReturnValue(of({ ...course, status: 'PUBLISHED' }));
    button('Publier la formation')!.click();
    harness.detectChanges();
    expect(editor.publishCourse).toHaveBeenCalledWith(10);
    expect(root().querySelector('.head .hg-tag')!.textContent).toContain('Publiée');
    expect(text()).toContain('elle apparaît dans le catalogue');

    editor.unpublishCourse.mockReturnValue(of({ ...course, status: 'DRAFT' }));
    button('Dépublier la formation')!.click();
    harness.detectChanges();
    expect(editor.unpublishCourse).toHaveBeenCalledWith(10);
    expect(text()).toContain('les étudiants inscrits n’y ont plus accès');
  });

  it('explains a failed publication', async () => {
    await open();
    editor.publishCourse.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 403 })));
    button('Publier la formation')!.click();
    harness.detectChanges();
    expect(text()).toContain("Vous n'avez pas le droit de modifier cette formation.");
  });

  it('lists modules and lessons with their status and links to the lesson editor', async () => {
    await open();
    expect(modules()).toHaveLength(2);
    expect(modules()[0].textContent).toContain('Module 1 · Se présenter');
    expect(modules()[0].textContent).toContain('Publié');
    expect(modules()[1].textContent).toContain('Brouillon');
    expect(modules()[1].textContent).toContain('Aucune leçon dans ce module.');
    const edit = modules()[0].querySelector<HTMLAnchorElement>('.lesson a')!;
    expect(edit.getAttribute('href')).toBe('/trainer/courses/10/lessons/11');
  });

  it('publishes a draft module and a draft lesson', async () => {
    await open();
    editor.publishModule.mockReturnValue(of({ ...M2, status: 'PUBLISHED' }));
    button('Publier le module', modules()[1])!.click();
    harness.detectChanges();
    expect(editor.publishModule).toHaveBeenCalledWith(2);
    expect(button('Publier le module')).toBeUndefined();

    editor.publishLesson.mockReturnValue(of({ ...L12, status: 'PUBLISHED' }));
    button('Publier', root().querySelectorAll('.lesson')[1])!.click();
    harness.detectChanges();
    expect(editor.publishLesson).toHaveBeenCalledWith(12);
    expect(root().querySelectorAll('.lesson')[1].textContent).toContain('Publiée');
  });

  it('adds a module at the end', async () => {
    await open();
    button('+ Ajouter un module')!.click();
    harness.detectChanges();
    const form = modules()[2].querySelector<HTMLFormElement>('form')!;
    type(form.querySelector('input')!, 'Les réunions');
    editor.createModule.mockReturnValue(of(module(3, 3, 'Les réunions', 'DRAFT')));
    submit(form);

    expect(editor.createModule).toHaveBeenCalledWith(10, { title: 'Les réunions', description: null });
    expect(modules()).toHaveLength(3);
    expect(modules()[2].textContent).toContain('Module 3 · Les réunions');
  });

  it('renames a module', async () => {
    await open();
    button('Renommer', modules()[0])!.click();
    harness.detectChanges();
    const form = modules()[0].querySelector<HTMLFormElement>('form')!;
    expect(form.querySelector('input')!.value).toBe('Se présenter');
    type(form.querySelector('input')!, 'Se présenter en réunion');
    editor.updateModule.mockReturnValue(of({ ...M1, title: 'Se présenter en réunion' }));
    submit(form);

    expect(editor.updateModule).toHaveBeenCalledWith(1, { title: 'Se présenter en réunion', description: null });
    expect(modules()[0].textContent).toContain('Module 1 · Se présenter en réunion');
  });

  it('adds a lesson to a module', async () => {
    await open();
    button('+ Ajouter une leçon', modules()[1])!.click();
    harness.detectChanges();
    const form = modules()[1].querySelector<HTMLFormElement>('form')!;
    type(form.querySelector('input')!, 'Prendre un message');
    editor.createLesson.mockReturnValue(of(lesson(21, 2, 'Prendre un message', 'DRAFT')));
    submit(form);

    expect(editor.createLesson).toHaveBeenCalledWith(2, { title: 'Prendre un message', description: null, content: null });
    expect(modules()[1].textContent).toContain('Prendre un message');
    expect(modules()[1].textContent).not.toContain('Aucune leçon');
  });

  it('does not create a lesson without title', async () => {
    await open();
    button('+ Ajouter une leçon', modules()[1])!.click();
    harness.detectChanges();
    submit(modules()[1].querySelector<HTMLFormElement>('form')!);
    expect(editor.createLesson).not.toHaveBeenCalled();
  });

  it('explains a 403 when the course is not assigned', async () => {
    load.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 403 })));
    await open();
    expect(text()).toContain("Cette formation ne vous est pas attribuée.");
  });
});
