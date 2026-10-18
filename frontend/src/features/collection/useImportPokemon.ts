import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useMemo } from 'react'

import { ApiError } from '@/api/api-error'
import { importPokemon } from '@/api/collection'
import { createApiClient } from '@/api/client'
import type { LocalPokemon } from '@/api/types'
import { useAnnouncer } from '@/app/useAnnouncer'
import { useAuth } from '@/features/auth/useAuth'

export type ImportPokemonVariables = {
  nationalNumber: number
}

export function useImportPokemon() {
  const { getToken } = useAuth()
  const queryClient = useQueryClient()
  const { announce } = useAnnouncer()

  const client = useMemo(
    () => createApiClient({ getToken }),
    [getToken],
  )

  return useMutation<LocalPokemon, Error, ImportPokemonVariables>({
    mutationFn: ({ nationalNumber }) =>
      importPokemon(client, { nationalNumber }),
    meta: { protectedWrite: true },
    onSuccess: async (data) => {
      await queryClient.invalidateQueries({ queryKey: ['collection'] })
      announce(`${data.name} added to the collection`)
    },
    onError: async (error) => {
      if (error instanceof ApiError && error.status === 409) {
        await queryClient.invalidateQueries({ queryKey: ['collection'] })
        announce('Already in the collection')
      }
    },
  })
}
