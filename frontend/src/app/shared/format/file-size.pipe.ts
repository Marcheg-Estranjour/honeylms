import { Pipe, PipeTransform } from '@angular/core';

const UNITS = ['o', 'Ko', 'Mo', 'Go'] as const;
const NUMBER = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 1 });

/** 1536 → « 1,5 Ko », 52428800 → « 50 Mo » (French units, base 1024 like the 50 MiB rule). */
export function formatFileSize(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes < 0) return '';
  let value = bytes;
  let unit = 0;
  while (value >= 1024 && unit < UNITS.length - 1) {
    value /= 1024;
    unit++;
  }
  return `${NUMBER.format(value)} ${UNITS[unit]}`;
}

@Pipe({ name: 'fileSize' })
export class FileSizePipe implements PipeTransform {
  transform(bytes: number | null | undefined): string {
    return bytes == null ? '' : formatFileSize(bytes);
  }
}

/** « rapport.final.PDF » → « PDF » ; no extension → « FICHIER ». */
export function fileTypeLabel(fileName: string): string {
  const dot = fileName.lastIndexOf('.');
  return dot > 0 && dot < fileName.length - 1 ? fileName.slice(dot + 1).toUpperCase() : 'FICHIER';
}
