import { Component } from '@angular/core';

/**
 * Two-column layout shared by the login and register pages
 * (mock-up « Connexion »): brand panel on the left, form on the right.
 */
@Component({
  selector: 'app-auth-layout',
  template: `
    <div class="auth">
      <aside class="brand" aria-hidden="true">
        <div class="logo-box">
          <img src="honey-group-logo.png" alt="" />
        </div>
        <div class="pitch">
          <p class="kicker">HONEY GROUP ACADEMY</p>
          <p class="hg-serif headline">Apprenez à votre rythme, où que vous soyez.</p>
          <ul class="domains">
            <li>Langues</li>
            <li>Bureautique</li>
            <li>EDUCTOUR</li>
          </ul>
        </div>
        <div class="road"></div>
      </aside>
      <main class="content">
        <ng-content />
      </main>
    </div>
  `,
  styleUrl: './auth-layout.scss',
})
export class AuthLayout {}
