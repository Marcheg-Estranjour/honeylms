import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Role, UserSummary } from '../auth/auth.models';
import { AuthService } from '../auth/auth.service';
import { Shell } from './shell';

describe('Shell', () => {
  const logout = vi.fn();

  function render(role: Role) {
    const user: UserSummary = { id: 1, email: 'x@example.com', firstName: 'camille', lastName: 'Martin', role };
    TestBed.configureTestingModule({
      imports: [Shell],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: { currentUser: signal(user), role: signal(role), logout },
        },
      ],
    });
    const fixture = TestBed.createComponent(Shell);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  const navLabels = (el: HTMLElement) =>
    Array.from(el.querySelectorAll('nav a')).map((a) => a.textContent?.trim());

  beforeEach(() => logout.mockReset());

  it('shows the student navigation, the user and the initials', () => {
    const el = render('STUDENT');
    expect(navLabels(el)).toEqual(['Catalogue', 'Mes cours']);
    expect(el.textContent).toContain('camille Martin');
    expect(el.querySelector('.avatar')?.textContent?.trim()).toBe('CM');
  });

  it('shows the trainer navigation (no course creation entry)', () => {
    expect(navLabels(render('TRAINER'))).toEqual(['Mes formations', 'Corrections']);
  });

  it('shows the admin navigation', () => {
    expect(navLabels(render('ADMIN'))).toEqual(['Utilisateurs', 'Formations']);
  });

  it('logs out and goes to the login page', () => {
    const el = render('STUDENT');
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    el.querySelector<HTMLButtonElement>('.user button')!.click();

    expect(logout).toHaveBeenCalledOnce();
    expect(navigate).toHaveBeenCalledWith('/login');
  });
});
