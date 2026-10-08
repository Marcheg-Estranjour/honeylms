import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { FileDownloadService } from './file-download.service';

describe('FileDownloadService', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    // jsdom does not implement object URLs.
    URL.createObjectURL = vi.fn(() => 'blob:fake');
    URL.revokeObjectURL = vi.fn();
  });

  afterEach(() => http.verify());

  it('fetches the file as a blob and saves it under its original name', () => {
    const click = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
    let done = false;

    TestBed.inject(FileDownloadService)
      .download('/api/resources/7/download', 'vocabulaire.pdf')
      .subscribe(() => (done = true));

    const req = http.expectOne('/api/resources/7/download');
    expect(req.request.responseType).toBe('blob');
    req.flush(new Blob(['%PDF']));

    expect(done).toBe(true);
    expect(URL.createObjectURL).toHaveBeenCalled();
    const link = click.mock.instances[0] as unknown as HTMLAnchorElement;
    expect(link.download).toBe('vocabulaire.pdf');
    expect(document.querySelector('a[download]')).toBeNull(); // removed after the click
    click.mockRestore();
  });
});
