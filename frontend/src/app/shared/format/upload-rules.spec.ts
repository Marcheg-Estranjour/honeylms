import { ACCEPT_ATTRIBUTE, ALLOWED_FORMATS_LABEL, checkUploadFile, MAX_UPLOAD_BYTES } from './upload-rules';

/** A File whose reported size is `size` without allocating it. */
function fakeFile(name: string, size: number): File {
  const file = new File(['x'], name);
  Object.defineProperty(file, 'size', { value: size });
  return file;
}

describe('upload rules', () => {
  it('accepts the allowed formats whatever the case', () => {
    expect(checkUploadFile(fakeFile('devoir.pdf', 1000))).toBeNull();
    expect(checkUploadFile(fakeFile('Photo.JPEG', 1000))).toBeNull();
    expect(checkUploadFile(fakeFile('suivi.ventes.xlsx', MAX_UPLOAD_BYTES))).toBeNull();
  });

  it('rejects other formats and files without extension', () => {
    expect(checkUploadFile(fakeFile('script.exe', 1000))).toContain('Format non accepté');
    expect(checkUploadFile(fakeFile('README', 1000))).toContain('Format non accepté');
    expect(checkUploadFile(fakeFile('.pdf', 1000))).toContain('Format non accepté');
  });

  it('rejects empty and too large files', () => {
    expect(checkUploadFile(fakeFile('vide.pdf', 0))).toBe('Ce fichier est vide.');
    expect(checkUploadFile(fakeFile('film.mp4', MAX_UPLOAD_BYTES + 1))).toContain('Taille maximale : 50 Mo');
  });

  it('exposes the accept attribute and the help label', () => {
    expect(ACCEPT_ATTRIBUTE).toBe('.pdf,.docx,.xlsx,.pptx,.jpg,.jpeg,.png,.mp3,.mp4');
    expect(ALLOWED_FORMATS_LABEL).toBe('PDF, DOCX, XLSX, PPTX, JPG, PNG, MP3, MP4');
  });
});
