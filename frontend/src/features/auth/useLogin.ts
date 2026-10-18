import { useMutation } from '@tanstack/react-query'
import { useMemo } from 'react'

import { login } from '@/api/auth'
import { createApiClient } from '@/api/client'
import type { AuthResponse, LoginRequest } from '@/api/types'

export function useLogin() {
  const client = useMemo(
    () => createApiClient({ getToken: () => null }),
    [],
  )

  return useMutation<AuthResponse, Error, LoginRequest>({
    mutationFn: (body) => login(client, body),
  })
}
