import { expect, test } from '@playwright/test';
import {
  ADMIN,
  ASSIGNMENT,
  ASSIGNMENT_LESSON,
  ENGLISH_COURSE,
  login,
  logout,
  TRAINER,
  uniqueStudent,
} from './helpers';

/**
 * The three critical journeys of the demo, chained on ONE new student:
 *   student: register → enroll → complete a lesson → submit the assignment
 *   trainer: grade the submission → the student sees the correction
 *   admin:   deactivate the student → he can no longer log in
 */
test.describe.serial('main journey', () => {
  const student = uniqueStudent();
  const studentName = `${student.firstName} ${student.lastName}`;
  let assignmentUrl = '';

  test('student: register, enroll, complete a lesson and submit the assignment', async ({ page }) => {
    // Register (auto-login after registration)
    await page.goto('/register');
    await page.locator('#firstName').fill(student.firstName);
    await page.locator('#lastName').fill(student.lastName);
    await page.locator('#email').fill(student.email);
    await page.locator('#password').fill(student.password);
    await page.locator('#confirmPassword').fill(student.password);
    await page.getByRole('button', { name: 'Créer mon compte' }).click();
    await expect(page).toHaveURL(/\/my-courses$/);

    // Enroll from the catalogue
    await page.goto('/catalog');
    await page.getByRole('link', { name: new RegExp(ENGLISH_COURSE) }).click();
    await page.getByRole('button', { name: "S'inscrire à ce cours" }).click();
    await page.getByRole('link', { name: 'Accéder au cours' }).click();

    // First lesson (no resume point yet) → mark it as completed
    await expect(page).toHaveURL(/\/lessons\/\d+$/);
    await expect(page.getByRole('heading', { level: 1, name: 'Se présenter' })).toBeVisible();
    await page.getByRole('button', { name: 'Marquer comme terminée' }).click();
    await expect(page.getByText('Leçon terminée')).toBeVisible();

    // Go to the lesson holding the assignment, then to the assignment
    await page.getByRole('link', { name: ASSIGNMENT_LESSON, exact: true }).click();
    await page.locator('a.assignment').click();
    await expect(page.getByRole('heading', { level: 1, name: ASSIGNMENT })).toBeVisible();
    await expect(page.locator('.hg-tag', { hasText: 'À rendre' })).toBeVisible();
    assignmentUrl = page.url();

    // Submit a PDF (two steps: choose, then confirm)
    await page.locator('input[type=file]').setInputFiles({
      name: 'relance-client.pdf',
      mimeType: 'application/pdf',
      buffer: Buffer.from('%PDF-1.4\n% HoneyLMS E2E\n'),
    });
    await page.getByRole('button', { name: 'Déposer ce fichier' }).click();
    await expect(page.getByText('Votre devoir a bien été déposé.')).toBeVisible();
    await expect(page.locator('.hg-tag', { hasText: 'Rendu' })).toBeVisible();
    await expect(page.getByText('En attente de correction.')).toBeVisible();
  });

  test('trainer: grade the submission, then the student sees the correction', async ({ page, browser }) => {
    await login(page, TRAINER.email);
    await page.goto('/trainer/submissions');
    await page.getByRole('link', { name: `Ouvrir ${ASSIGNMENT}` }).click();

    await page.getByRole('button', { name: new RegExp(studentName) }).click();
    await page.getByLabel('Note /20 (facultative)').fill('14,5');
    await page.getByLabel('Commentaire du formateur').fill('Ton professionnel, bonne relance.');
    await page.getByRole('button', { name: 'Enregistrer la correction' }).click();
    await expect(page.getByText('Correction enregistrée')).toBeVisible();

    // The student, in a separate browser context (own session)
    const studentContext = await browser.newContext();
    const studentPage = await studentContext.newPage();
    await login(studentPage, student.email, student.password);
    await studentPage.goto(assignmentUrl);
    const correction = studentPage.locator('.correction');
    await expect(correction).toContainText('14,5');
    await expect(correction).toContainText('Ton professionnel, bonne relance.');
    await expect(correction).toContainText(`Corrigé par ${TRAINER.name}`);
    await studentContext.close();
  });

  test('admin: deactivate the student, who can no longer log in', async ({ page }) => {
    await login(page, ADMIN.email);
    await page.goto('/admin/users');
    await page.getByLabel('Rechercher un utilisateur').fill(student.email);
    await page.getByRole('button', { name: `Désactiver ${studentName}` }).click();
    await expect(page.getByText(`Le compte de ${studentName} est désactivé`)).toBeVisible();
    await logout(page);

    await page.locator('#email').fill(student.email);
    await page.locator('#password').fill(student.password);
    await page.getByRole('button', { name: 'Se connecter' }).click();
    await expect(page.getByText('Ce compte est désactivé. Contactez un administrateur.')).toBeVisible();
  });
});
