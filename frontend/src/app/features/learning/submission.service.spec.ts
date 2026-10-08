import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { SubmissionDetail } from './learning.models';
import { SubmissionService } from './submission.service';

describe('SubmissionService', () => {
  let service: SubmissionService;
  let http: HttpTestingController;
  const file = new File(['hello'], 'relance.pdf', { type: 'application/pdf' });

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(SubmissionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the assignment', () => {
    service.getAssignment(30).subscribe();
    http.expectOne('/api/assignments/30').flush({});
  });

  it('maps « no submission yet » (404) to null', () => {
    let result: SubmissionDetail | null | undefined;
    service.getMySubmission(30).subscribe((s) => (result = s));
    http.expectOne('/api/assignments/30/submissions/me').flush({}, { status: 404, statusText: 'Not Found' });
    expect(result).toBeNull();
  });

  it('propagates the other errors', () => {
    let status = 0;
    service.getMySubmission(30).subscribe({ error: (e) => (status = e.status) });
    http.expectOne('/api/assignments/30/submissions/me').flush({}, { status: 403, statusText: 'Forbidden' });
    expect(status).toBe(403);
  });

  it('submits the file as multipart part « file »', () => {
    service.submit(30, file).subscribe();
    const req = http.expectOne('/api/assignments/30/submissions');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toBeInstanceOf(FormData);
    expect((req.request.body as FormData).get('file')).toBeInstanceOf(File);
    expect(req.request.headers.has('Content-Type')).toBe(false);
    req.flush({});
  });

  it('replaces the file with PUT', () => {
    service.replace(12, file).subscribe();
    const req = http.expectOne('/api/submissions/12');
    expect(req.request.method).toBe('PUT');
    expect(((req.request.body as FormData).get('file') as File).name).toBe('relance.pdf');
    req.flush({});
  });
});
