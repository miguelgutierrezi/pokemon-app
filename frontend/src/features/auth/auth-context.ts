import { createContext } from 'react'

import type { AuthResponse, AuthUser } from '@/api/types'

export type AuthContextValue = {
  user: AuthUser | null
  isAuthenticated: boolean
  getToken: () => string | null
  signIn: (authResponse: AuthResponse) => void
  signOut: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
