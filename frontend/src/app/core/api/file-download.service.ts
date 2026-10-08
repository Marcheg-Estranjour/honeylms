import { HttpClient } from '@angular/common/http';
import { DOCUMENT } from '@angular/common';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

/**
 * Downloads a protected file and hands it to the browser.
 *
 * CHOIX TECHNIQUE : download endpoints require the JWT (Authorization header). A plain
 * <a href="/api/..."> cannot send that header, and putting the token in the URL would leak
 * it (history, logs). So the file is fetched with HttpClient (the auth interceptor adds the
 * header), turned into a temporary object URL, and saved through a hidden <a download>.
 * Limit (acceptable for the MVP, 50 MiB max): the whole file goes through browser memory.
 */
@Injectable({ providedIn: 'root' })
export class FileDownloadService {
  private readonly http = inject(HttpClient);
  private readonly document = inject(DOCUMENT);

  download(url: string, fileName: string): Observable<void> {
    return this.http.get(url, { responseType: 'blob' }).pipe(map((blob) => this.save(blob, fileName)));
  }

  private save(blob: Blob, fileName: string): void {
    const objectUrl = URL.createObjectURL(blob);
    const link = this.document.createElement('a');
    link.href = objectUrl;
    link.download = fileName;
    link.style.display = 'none';
    this.document.body.appendChild(link);
    link.click();
    link.remove();
    // Let the browser start the download before releasing the memory.
    setTimeout(() => URL.revokeObjectURL(objectUrl), 0);
  }
}
