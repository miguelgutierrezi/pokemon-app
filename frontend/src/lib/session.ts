import type { AuthResponse, AuthUser } from '@/api/types'

export const SESSION_KEY = 'pokefolio.session'

export type SessionSnapshot = {
  token: string
  expiresAt: number
  user: AuthUser
}

function isAuthUser(value: unknown): value is AuthUser {
  if (typeof value !== 'object' || value === null) {
    return false
  }
  const user = value as Record<string, unknown>
  return (
    typeof user.id === 'number' &&
    typeof user.name === 'string' &&
    typeof user.email === 'string'
  )
}

function isSessionSnapshot(value: unknown): value is SessionSnapshot {
  if (typeof value !== 'object' || value === null) {
    return false
  }
  const session = value as Record<string, unknown>
  return (
    typeof session.token === 'string' &&
    session.token.length > 0 &&
    typeof session.expiresAt === 'number' &&
    Number.isFinite(session.expiresAt) &&
    isAuthUser(session.user)
  )
}

function readRaw(): string | null {
  try {
    return localStorage.getItem(SESSION_KEY)
  } catch {
    return null
  }
}

function writeRaw(value: string): void {
  try {
    localStorage.setItem(SESSION_KEY, value)
  } catch {
    // Quota / privacy mode — treat as signed out for callers.
  }
}

export function clear(): void {
  try {
    localStorage.removeItem(SESSION_KEY)
  } catch {
    // ignore
  }
}

export function save(authResponse: AuthResponse, now: number): void {
  const snapshot: SessionSnapshot = {
    token: authResponse.accessToken,
    expiresAt: now + authResponse.expiresIn * 1000,
    user: authResponse.user,
  }
  writeRaw(JSON.stringify(snapshot))
}

export function load(now: number): SessionSnapshot | null {
  const raw = readRaw()
  if (raw == null || raw.length === 0) {
    return null
  }

  let parsed: unknown
  try {
    parsed = JSON.parse(raw) as unknown
  } catch {
    clear()
    return null
  }

  if (!isSessionSnapshot(parsed)) {
    clear()
    return null
  }

  if (now >= parsed.expiresAt) {
    clear()
    return null
  }

  return parsed
}

export function subscribe(
  listener: (session: SessionSnapshot | null) => void,
): () => void {
  const onStorage = (event: StorageEvent) => {
    if (event.storageArea && event.storageArea !== localStorage) {
      return
    }
    // Another tab called localStorage.clear() — key is null.
    if (event.key === null) {
      listener(null)
      return
    }
    if (event.key !== SESSION_KEY) {
      return
    }
    listener(load(Date.now()))
  }

  window.addEventListener('storage', onStorage)
  return () => {
    window.removeEventListener('storage', onStorage)
  }
}
