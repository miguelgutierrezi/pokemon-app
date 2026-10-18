import { useQuery } from '@tanstack/react-query'

import { getLocalPokemonById } from '@/api/collection'
import { localPokemonQueryKey } from '@/features/collection/query-keys'

export function useLocalPokemon(id: number | null) {
  return useQuery({
    queryKey: localPokemonQueryKey(id ?? 0),
    queryFn: ({ signal }) => getLocalPokemonById(id!, signal),
    enabled: id != null,
    retry: false,
  })
}
