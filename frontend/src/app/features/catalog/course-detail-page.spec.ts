import { registerLocaleData } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import localeFr from '@angular/common/locales/fr';
import { LOCALE_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { CourseDetail, EnrolledCourse } from '../../shared/courses/course.models';
import { EnrollmentService } from '../learning/enrollment.service';
import { CatalogService } from './catalog.service';
import { CourseDetailPage } from './course-detail-page';

registerLocaleData(localeFr);

describe('CourseDetailPage', () => {
  const getCourse = vi.fn();
  const getMyCourses = vi.fn();
  const enroll = vi.fn();
  let harness: RouterTestingHarness;

  const course: CourseDetail = {
    id: 4,
    title: 'Excel — Les essentiels',
    description: 'Formules, mise en forme et premiers tableaux croisés.',
    category: 'OFFICE_AUTOMATION',
    status: 'PUBLISHED',
    createdByUserId: 1,
  };
  const enrolledInExcel: EnrolledCourse = {
    courseId: 4,
    title: course.title,
    description: null,
    category: 'OFFICE_AUTOMATION',
    enrolledAt: '2026-09-28T08:00:00Z',
  };

  async function open() {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'courses/:courseId', component: CourseDetailPage }]),
        { provide: CatalogService, useValue: { getCourse } },
        { provide: EnrollmentService, useValue: { getMyCourses, enroll } },
        { provide: LOCALE_ID, useValue: 'fr-FR' },
      ],
    });
    harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/courses/4', CourseDetailPage);
  }

  const root = () => harness.routeNativeElement!;
  const text = () => root().textContent ?? '';
  const clickEnroll = () => {
    root().querySelector<HTMLButtonElement>('button.cta')!.click();
    harness.detectChanges();
  };

  beforeEach(() => {
    getCourse.mockReset().mockReturnValue(of(course));
    getMyCourses.mockReset().mockReturnValue(of([]));
    enroll.mockReset();
  });

  it('shows the course with its domain and the enroll button', async () => {
    await open();
    expect(getCourse).toHaveBeenCalledWith(4);
    expect(text()).toContain('Excel — Les essentiels');
    expect(text()).toContain('Bureautique');
    expect(text()).toContain("S'inscrire à ce cours");
    expect(text()).toContain('visibles après inscription');
  });

  it('enrolls the student and offers to open the course', async () => {
    await open();
    enroll.mockReturnValue(of({ courseId: 4, enrolledAt: '2026-10-06T10:00:00Z' }));
    clickEnroll();

    expect(enroll).toHaveBeenCalledWith(4);
    expect(text()).toContain('Inscription confirmée');
    const cta = root().querySelector<HTMLAnchorElement>('a.cta')!;
    expect(cta.textContent).toContain('Accéder au cours');
    expect(cta.getAttribute('href')).toBe('/courses/4/learn');
  });

  it('shows « Accéder au cours » directly when already enrolled', async () => {
    getMyCourses.mockReturnValue(of([enrolledInExcel]));
    await open();
    expect(root().querySelector('button.cta')).toBeNull();
    expect(text()).toContain('Accéder au cours');
    expect(text()).toContain('Inscrit depuis le 28 septembre 2026');
  });

  it('treats a 409 as « already enrolled », not as an error', async () => {
    await open();
    enroll.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 409 })));
    getMyCourses.mockReturnValue(of([enrolledInExcel]));
    clickEnroll();

    expect(root().querySelector('.alert.error')).toBeNull();
    expect(text()).toContain('Accéder au cours');
  });

  it('explains a 403 on enrollment', async () => {
    await open();
    enroll.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 403 })));
    clickEnroll();
    expect(text()).toContain("Ce cours n'est pas ouvert aux inscriptions.");
  });

  it('shows « course not found » on a 404', async () => {
    getCourse.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
    await open();
    expect(text()).toContain("Ce cours n'existe pas.");
    expect(text()).toContain('Retour au catalogue');
  });
});
