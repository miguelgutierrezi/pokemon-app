import { QueryClientProvider } from '@tanstack/react-query'
import { useEffect, useState, type ReactNode } from 'react'

import { AnnouncerProvider } from '@/app/announcer'
import {
  createAppQueryClient,
  setQueryClientSignOut,
} from '@/app/query-client'
import { AuthProvider } from '@/features/auth/AuthProvider'
import { useAuth } from '@/features/auth/useAuth'

function QueryProvider({ children }: { children: ReactNode }) {
  const { signOut } = useAuth()
  const [queryClient] = useState(() => createAppQueryClient())

  useEffect(() => {
    setQueryClientSignOut(signOut)
  }, [signOut])

  return (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  )
}

export function AppProviders({ children }: { children: ReactNode }) {
  return (
    <AuthProvider>
      <AnnouncerProvider>
        <QueryProvider>{children}</QueryProvider>
      </AnnouncerProvider>
    </AuthProvider>
  )
}
