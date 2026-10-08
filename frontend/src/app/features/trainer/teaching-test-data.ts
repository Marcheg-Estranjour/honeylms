import { ManagedAssignmentSummary, ManagedCourseSummary } from './teaching.models';

/** Fixtures shared by the trainer page specs. */
export const ENGLISH: ManagedCourseSummary = {
  id: 10,
  title: 'Anglais professionnel — B1',
  description: null,
  category: 'LANGUAGES',
  status: 'PUBLISHED',
  enrolledStudents: 3,
  submissionsToCorrect: 2,
};

export const WORD: ManagedCourseSummary = {
  id: 12,
  title: 'Word — les bases',
  description: null,
  category: 'OFFICE_AUTOMATION',
  status: 'DRAFT',
  enrolledStudents: 0,
  submissionsToCorrect: 0,
};

export function managedAssignment(
  id: number,
  overrides: Partial<ManagedAssignmentSummary> = {},
): ManagedAssignmentSummary {
  return {
    id,
    title: `Devoir ${id}`,
    dueDate: '2099-10-30T22:59:00Z',
    status: 'PUBLISHED',
    courseId: ENGLISH.id,
    courseTitle: ENGLISH.title,
    lessonId: 100 + id,
    lessonTitle: `Leçon ${id}`,
    enrolledStudents: 3,
    submissions: 0,
    submissionsToCorrect: 0,
    ...overrides,
  };
}
