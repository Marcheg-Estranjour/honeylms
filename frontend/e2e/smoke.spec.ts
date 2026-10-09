import { expect, test } from '@playwright/test';
import { login, TRAINER } from './helpers';

test('a visitor is sent to the login page', async ({ page }) => {
  await page.goto('/my-courses');
  await expect(page).toHaveURL(/\/login\?returnUrl=/);
});

test('a wrong password shows a French message', async ({ page }) => {
  await page.goto('/login');
  await page.locator('#email').fill(TRAINER.email);
  await page.locator('#password').fill('mauvais-mot-de-passe');
  await page.getByRole('button', { name: 'Se connecter' }).click();
  await expect(page.getByText('Email ou mot de passe incorrect')).toBeVisible();
});

test('each role lands on its home page', async ({ page }) => {
  await login(page, TRAINER.email);
  await expect(page).toHaveURL(/\/trainer\/courses$/);
  await expect(page.getByRole('heading', { name: 'Mes formations' })).toBeVisible();
});
