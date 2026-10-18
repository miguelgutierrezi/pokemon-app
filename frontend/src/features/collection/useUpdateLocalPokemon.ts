import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useMemo } from 'react'

import { updateLocalPokemon } from '@/api/collection'
import { createApiClient } from '@/api/client'
import type { LocalPokemon, UpdateLocalPokemonRequest } from '@/api/types'
import { useAnnouncer } from '@/app/useAnnouncer'
import { useAuth } from '@/features/auth/useAuth'

export type UpdateLocalPokemonVariables = {
  id: number
  body: UpdateLocalPokemonRequest
}

export function useUpdateLocalPokemon() {
  const { getToken } = useAuth()
  const queryClient = useQueryClient()
  const { announce } = useAnnouncer()

  const client = useMemo(
    () => createApiClient({ getToken }),
    [getToken],
  )

  return useMutation<LocalPokemon, Error, UpdateLocalPokemonVariables>({
    mutationFn: ({ id, body }) => updateLocalPokemon(client, id, body),
    meta: { protectedWrite: true },
    onSuccess: async (data) => {
      await queryClient.invalidateQueries({ queryKey: ['collection'] })
      announce(`${data.name} updated in the collection`)
    },
  })
}
