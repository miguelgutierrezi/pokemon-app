import { sanitizeReturnPath } from '@/lib/return-path'

/** Build a same-page auth link that preserves a sanitised `redirect` query. */
export function authLinkWithRedirect(
  path: '/login' | '/register',
  redirect: string | null,
): string {
  if (redirect == null || redirect.trim() === '') {
    return path
  }
  const safe = sanitizeReturnPath(redirect)
  if (safe === '/') {
    return path
  }
  return `${path}?redirect=${encodeURIComponent(safe)}`
}

export function resolvePostAuthPath(redirect: string | null): string {
  return sanitizeReturnPath(redirect)
}
