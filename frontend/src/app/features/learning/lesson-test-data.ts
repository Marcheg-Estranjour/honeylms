// Shared fixtures for the lesson-view specs (not part of the application bundle: only imported by *.spec.ts).
import { CourseDetail } from '../../shared/courses/course.models';
import { CourseOutline } from './course-outline.service';
import { LessonDetail, ModuleDetail } from './learning.models';

export const COURSE: CourseDetail = {
  id: 1,
  title: 'Anglais professionnel — B1',
  description: null,
  category: 'LANGUAGES',
  status: 'PUBLISHED',
  createdByUserId: 1,
};

export const moduleOf = (id: number, displayOrder: number, title: string): ModuleDetail => ({
  id,
  courseId: 1,
  title,
  description: null,
  displayOrder,
  status: 'PUBLISHED',
});

export const lessonOf = (id: number, moduleId: number, displayOrder: number, title: string, content: string | null = null): LessonDetail => ({
  id,
  moduleId,
  title,
  description: null,
  content,
  displayOrder,
  status: 'PUBLISHED',
});

export const M1 = moduleOf(10, 1, 'Se présenter');
export const M2 = moduleOf(20, 2, 'Écrire des emails');
export const L11 = lessonOf(101, 10, 1, 'Se présenter', 'Hello, my name is Camille.');
export const L12 = lessonOf(102, 10, 2, 'Présenter son entreprise');
export const L21 = lessonOf(201, 20, 1, "Structure d'un email");
export const L22 = lessonOf(202, 20, 2, 'Le ton et les formules de politesse');

export const OUTLINE: CourseOutline = {
  course: COURSE,
  modules: [
    { module: M1, lessons: [L11, L12] },
    { module: M2, lessons: [L21, L22] },
  ],
  lessons: [L11, L12, L21, L22],
};
