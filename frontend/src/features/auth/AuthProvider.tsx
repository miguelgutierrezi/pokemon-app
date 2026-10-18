import {
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'

import type { AuthResponse } from '@/api/types'
import { AuthContext } from '@/features/auth/auth-context'
import {
  clear as clearSession,
  load,
  save,
  subscribe,
  type SessionSnapshot,
} from '@/lib/session'

/** setTimeout delay must fit a signed 32-bit int. */
const MAX_TIMEOUT_MS = 2 ** 31 - 1

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<SessionSnapshot | null>(() =>
    load(Date.now()),
  )

  const signOut = useCallback(() => {
    clearSession()
    setSession(null)
  }, [])

  const applySnapshot = useCallback((snapshot: SessionSnapshot | null) => {
    setSession(snapshot)
  }, [])

  const signIn = useCallback((authResponse: AuthResponse) => {
    const now = Date.now()
    save(authResponse, now)
    setSession({
      token: authResponse.accessToken,
      expiresAt: now + authResponse.expiresIn * 1000,
      user: authResponse.user,
    })
  }, [])

  const getToken = useCallback(
    (): string | null => load(Date.now())?.token ?? null,
    [],
  )

  useEffect(() => {
    if (session == null) {
      return
    }
    const delay = Math.min(
      Math.max(session.expiresAt - Date.now(), 0),
      MAX_TIMEOUT_MS,
    )
    const timerId = window.setTimeout(() => {
      signOut()
    }, delay)
    return () => {
      window.clearTimeout(timerId)
    }
  }, [session, signOut])

  useEffect(() => subscribe(applySnapshot), [applySnapshot])

  const value = useMemo(
    () => ({
      user: session?.user ?? null,
      isAuthenticated: session != null,
      getToken,
      signIn,
      signOut,
    }),
    [session, getToken, signIn, signOut],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
