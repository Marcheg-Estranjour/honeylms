import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AdminUsersService } from './admin-users.service';

describe('AdminUsersService', () => {
  let service: AdminUsersService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(AdminUsersService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists users, creates a trainer and changes a status', () => {
    const trainer = { email: 'a@b.fr', password: 'Secret123!', firstName: 'A', lastName: 'B' };
    service.listUsers().subscribe();
    service.createTrainer(trainer).subscribe();
    service.setActive(4, false).subscribe();

    http.expectOne('/api/users').flush([]);
    const create = http.expectOne('/api/users/trainers');
    expect(create.request.method).toBe('POST');
    expect(create.request.body).toEqual(trainer);
    create.flush({});
    const status = http.expectOne('/api/users/4/status');
    expect(status.request.method).toBe('PATCH');
    expect(status.request.body).toEqual({ active: false });
    status.flush({});
  });
});
