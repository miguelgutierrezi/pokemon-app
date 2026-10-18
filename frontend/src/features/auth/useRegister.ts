import { useMutation } from '@tanstack/react-query'
import { useMemo } from 'react'

import { register } from '@/api/auth'
import { createApiClient } from '@/api/client'
import type { AuthResponse, RegisterRequest } from '@/api/types'

export function useRegister() {
  const client = useMemo(
    () => createApiClient({ getToken: () => null }),
    [],
  )

  return useMutation<AuthResponse, Error, RegisterRequest>({
    mutationFn: (body) => register(client, body),
  })
}
