import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useMemo } from 'react'

import { ApiError } from '@/api/api-error'
import { deleteLocalPokemon } from '@/api/collection'
import { createApiClient } from '@/api/client'
import { useAnnouncer } from '@/app/useAnnouncer'
import { useAuth } from '@/features/auth/useAuth'
import { localPokemonQueryKey } from '@/features/collection/query-keys'

export type DeleteLocalPokemonVariables = {
  id: number
  name: string
}

function clearCollectionAfterDelete(
  queryClient: ReturnType<typeof useQueryClient>,
  id: number,
) {
  queryClient.removeQueries({ queryKey: localPokemonQueryKey(id) })
  return queryClient.invalidateQueries({ queryKey: ['collection', 'list'] })
}

export function useDeleteLocalPokemon() {
  const { getToken } = useAuth()
  const queryClient = useQueryClient()
  const { announce } = useAnnouncer()

  const client = useMemo(
    () => createApiClient({ getToken }),
    [getToken],
  )

  return useMutation<void, Error, DeleteLocalPokemonVariables>({
    mutationFn: ({ id }) => deleteLocalPokemon(client, id),
    meta: { protectedWrite: true },
    onSuccess: async (_data, variables) => {
      await clearCollectionAfterDelete(queryClient, variables.id)
      announce(`${variables.name} removed from the collection`)
    },
    onError: async (error, variables) => {
      if (error instanceof ApiError && error.status === 404) {
        await clearCollectionAfterDelete(queryClient, variables.id)
        announce(`${variables.name} was already removed from the collection`)
      }
    },
  })
}
