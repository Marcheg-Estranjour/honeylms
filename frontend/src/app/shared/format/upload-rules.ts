import { formatFileSize } from './file-size.pipe';

/*
 * Upload rules of the platform (Dossier de Conception, rule « fichiers ») mirrored on the client.
 *
 * CHOIX TECHNIQUE : this check is for comfort only (immediate French message, no useless 50 MiB
 * upload). The backend (FileStorageService) stays the authority and re-validates every file.
 * Keep in sync with `honeylms.storage.allowed-extensions` / `max-size-bytes`.
 */

export const ALLOWED_EXTENSIONS = ['pdf', 'docx', 'xlsx', 'pptx', 'jpg', 'jpeg', 'png', 'mp3', 'mp4'] as const;

/** 50 MiB, same base as the backend. */
export const MAX_UPLOAD_BYTES = 50 * 1024 * 1024;

/** Value for <input type="file" accept>: « .pdf,.docx,… ». */
export const ACCEPT_ATTRIBUTE = ALLOWED_EXTENSIONS.map((ext) => `.${ext}`).join(',');

/** « PDF, DOCX, XLSX, PPTX, JPG, PNG, MP3, MP4 » for the help text (JPEG is the same as JPG). */
export const ALLOWED_FORMATS_LABEL = ALLOWED_EXTENSIONS.filter((ext) => ext !== 'jpeg')
  .map((ext) => ext.toUpperCase())
  .join(', ');

/** Returns a French error message, or null when the file can be sent. */
export function checkUploadFile(file: File): string | null {
  const dot = file.name.lastIndexOf('.');
  const extension = dot > 0 ? file.name.slice(dot + 1).toLowerCase() : '';
  if (!(ALLOWED_EXTENSIONS as readonly string[]).includes(extension)) {
    return `Format non accepté. Formats possibles : ${ALLOWED_FORMATS_LABEL}.`;
  }
  if (file.size === 0) {
    return 'Ce fichier est vide.';
  }
  if (file.size > MAX_UPLOAD_BYTES) {
    return `Fichier trop volumineux (${formatFileSize(file.size)}). Taille maximale : ${formatFileSize(MAX_UPLOAD_BYTES)}.`;
  }
  return null;
}
