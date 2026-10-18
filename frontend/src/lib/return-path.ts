const FALLBACK = '/'
const SAME_ORIGIN_BASE = 'https://pokefolio.invalid'

/**
 * Returns a same-origin relative path suitable for post-login redirects.
 * Rejects open-redirect tricks (schemes, protocol-relative, backslashes,
 * and one-level percent-encoding of those).
 */
export function sanitizeReturnPath(
  value: string | null | undefined,
): string {
  if (value == null) {
    return FALLBACK
  }

  let candidate = String(value).trim()
  if (candidate.length === 0) {
    return FALLBACK
  }

  try {
    candidate = decodeURIComponent(candidate)
  } catch {
    return FALLBACK
  }

  candidate = candidate.trim()
  if (candidate.length === 0) {
    return FALLBACK
  }

  if (!candidate.startsWith('/') || candidate.startsWith('//')) {
    return FALLBACK
  }

  if (candidate.includes('\\')) {
    return FALLBACK
  }

  if (/^[a-zA-Z][a-zA-Z0-9+.-]*:/.test(candidate)) {
    return FALLBACK
  }

  try {
    const url = new URL(candidate, SAME_ORIGIN_BASE)
    if (url.origin !== SAME_ORIGIN_BASE) {
      return FALLBACK
    }
    return `${url.pathname}${url.search}${url.hash}` || FALLBACK
  } catch {
    return FALLBACK
  }
}
