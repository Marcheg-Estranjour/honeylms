import { expect, Page } from '@playwright/test';

/** Seed accounts (backend/src/main/resources/db/seed/R__demo_seed.sql). */
export const PASSWORD = 'Honey2026!';
export const TRAINER = { email: 'formateur@honeylms.test', name: 'Paul Durand' };
export const ADMIN = { email: 'admin@honeylms.test' };

export const ENGLISH_COURSE = 'Anglais professionnel — B1';
export const ASSIGNMENT = 'Rédiger un email de relance client';
export const ASSIGNMENT_LESSON = 'Rédiger un email de relance';

/** A brand-new student for each run: the tests never depend on what a previous run left. */
export function uniqueStudent() {
  const stamp = Date.now().toString(36);
  return {
    firstName: 'Emma',
    lastName: `Test${stamp}`,
    email: `e2e.${stamp}@honeylms.test`,
    password: 'E2e-Honey2026!',
  };
}

export async function login(page: Page, email: string, password = PASSWORD): Promise<void> {
  await page.goto('/login');
  await page.locator('#email').fill(email);
  await page.locator('#password').fill(password);
  await page.getByRole('button', { name: 'Se connecter' }).click();
  await expect(page).not.toHaveURL(/\/login/);
}

export async function logout(page: Page): Promise<void> {
  await page.getByRole('button', { name: 'Déconnexion' }).click();
  await expect(page).toHaveURL(/\/login/);
}
