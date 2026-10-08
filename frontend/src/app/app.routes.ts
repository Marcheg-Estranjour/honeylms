import { inject } from '@angular/core';
import { Routes } from '@angular/router';
import { authGuard, guestGuard, roleGuard } from './core/auth/auth.guards';
import { AuthService } from './core/auth/auth.service';
import { Shell } from './core/layout/shell';

const TITLE_SUFFIX = ' · Honey Group Academy';

/** Placeholder page for a screen planned in a later sprint (see ComingSoonPage). */
const comingSoon = (heading: string, sprint: string) => ({
  loadComponent: () => import('./shared/coming-soon-page').then((m) => m.ComingSoonPage),
  data: { heading, sprint },
  title: heading + TITLE_SUFFIX,
});

export const routes: Routes = [
  // ---- Public pages (no header) ----
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login-page').then((m) => m.LoginPage),
    title: 'Connexion' + TITLE_SUFFIX,
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/register-page').then((m) => m.RegisterPage),
    title: 'Créer un compte' + TITLE_SUFFIX,
  },

  // ---- Logged-in area (header + role navigation) ----
  {
    path: '',
    component: Shell,
    canActivate: [authGuard],
    children: [
      // "/" → home page of the current role (or /login when logged out).
      { path: '', pathMatch: 'full', redirectTo: () => inject(AuthService).homeUrl() },

      // Student
      {
        path: 'catalog',
        canActivate: [roleGuard('STUDENT')],
        loadComponent: () => import('./features/catalog/catalog-page').then((m) => m.CatalogPage),
        title: 'Catalogue' + TITLE_SUFFIX,
      },
      {
        path: 'courses/:courseId',
        canActivate: [roleGuard('STUDENT')],
        loadComponent: () =>
          import('./features/catalog/course-detail-page').then((m) => m.CourseDetailPage),
        title: 'Détail du cours' + TITLE_SUFFIX,
      },
      // Entry point of a course for an enrolled student: resume point, else first lesson.
      {
        path: 'courses/:courseId/learn',
        canActivate: [roleGuard('STUDENT')],
        loadComponent: () => import('./features/learning/learn-entry-page').then((m) => m.LearnEntryPage),
        title: 'Suivre le cours' + TITLE_SUFFIX,
      },
      {
        path: 'courses/:courseId/lessons/:lessonId',
        canActivate: [roleGuard('STUDENT')],
        loadComponent: () => import('./features/learning/lesson-page').then((m) => m.LessonPage),
        title: 'Leçon' + TITLE_SUFFIX,
      },
      {
        path: 'courses/:courseId/assignments/:assignmentId',
        canActivate: [roleGuard('STUDENT')],
        loadComponent: () => import('./features/learning/assignment-page').then((m) => m.AssignmentPage),
        title: 'Devoir' + TITLE_SUFFIX,
      },
      {
        path: 'my-courses',
        canActivate: [roleGuard('STUDENT')],
        loadComponent: () => import('./features/learning/my-courses-page').then((m) => m.MyCoursesPage),
        title: 'Mes cours' + TITLE_SUFFIX,
      },

      // Trainer
      { path: 'trainer/courses', canActivate: [roleGuard('TRAINER')], ...comingSoon('Mes formations', 'S9') },
      { path: 'trainer/submissions', canActivate: [roleGuard('TRAINER')], ...comingSoon('Corrections', 'S9') },

      // Admin
      { path: 'admin/users', canActivate: [roleGuard('ADMIN')], ...comingSoon('Utilisateurs', 'S10') },
      { path: 'admin/courses', canActivate: [roleGuard('ADMIN')], ...comingSoon('Formations', 'S10') },
    ],
  },

  { path: '**', redirectTo: '' },
];
