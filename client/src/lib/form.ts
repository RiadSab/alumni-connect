// Helpers for building multipart/form-data request bodies.

/** A FormData carrying a single file under the field name "file". */
export function singleFileForm(file: File): FormData {
  const form = new FormData();
  form.append("file", file);
  return form;
}

/** Append a value to FormData only when defined; booleans become "true"/"false". */
export function appendIfPresent(
  form: FormData,
  key: string,
  value: string | boolean | File | undefined,
): void {
  if (value === undefined) return;
  form.append(key, value instanceof File ? value : String(value));
}
