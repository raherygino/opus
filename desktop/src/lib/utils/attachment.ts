/**
 * Shared helpers for attachment files used across features
 * (personnel, mouvement, correspondance, …).
 */

const IMAGE_EXTENSIONS = [
  "jpg",
  "jpeg",
  "png",
  "gif",
  "webp",
  "bmp",
  "heic",
  "heif",
  "svg",
];

/**
 * Whether an attachment is an image that can be previewed in-app.
 * Uses the MIME type when available and falls back to the file extension.
 */
export function isImageFile(
  mimeType: string | null | undefined,
  filename: string | null | undefined,
): boolean {
  if (mimeType && mimeType.startsWith("image/")) return true;
  if (!filename) return false;
  const ext = filename.split(".").pop()?.toLowerCase();
  return !!ext && IMAGE_EXTENSIONS.includes(ext);
}
