import { ApiError } from '@/api/api-error'

/**
 * Parse a 1-based `?page=` query value.
 * Empty/missing → 1. Non-numeric, zero, or negative → null (invalid).
 */
export function parsePageParam(raw: string | null): number | null {
  if (raw == null || raw.trim() === '') {
    return 1
  }
  if (!/^\d+$/.test(raw)) {
    return null
  }
  const value = Number(raw)
  if (!Number.isInteger(value) || value < 1) {
    return null
  }
  return value
}

/** Same contract for catalog and collection: API 400 when page is past the last page. */
export function isBeyondLastPageError(error: unknown, page: number): boolean {
  return page > 1 && error instanceof ApiError && error.status === 400
}
