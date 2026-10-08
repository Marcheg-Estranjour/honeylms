import { fileTypeLabel, FileSizePipe, formatFileSize } from './file-size.pipe';

describe('file formatting', () => {
  it('formats sizes with French units', () => {
    expect(formatFileSize(0)).toBe('0 o');
    expect(formatFileSize(512)).toBe('512 o');
    expect(formatFileSize(1536)).toBe('1,5 Ko');
    expect(formatFileSize(52_428_800)).toBe('50 Mo');
    expect(formatFileSize(-1)).toBe('');
  });

  it('pipe ignores missing values', () => {
    expect(new FileSizePipe().transform(null)).toBe('');
    expect(new FileSizePipe().transform(2048)).toBe('2 Ko');
  });

  it('derives a type label from the file name', () => {
    expect(fileTypeLabel('vocabulaire.pdf')).toBe('PDF');
    expect(fileTypeLabel('rapport.final.Docx')).toBe('DOCX');
    expect(fileTypeLabel('README')).toBe('FICHIER');
    expect(fileTypeLabel('.env')).toBe('FICHIER');
  });
});
