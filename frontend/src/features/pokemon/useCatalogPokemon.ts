import { useQuery } from '@tanstack/react-query'

import { getPokemonById } from '@/api/pokemon'
import { catalogPokemonQueryKey } from '@/features/pokemon/query-keys'

export function useCatalogPokemon(id: number | null) {
  return useQuery({
    queryKey: catalogPokemonQueryKey(id ?? 0),
    queryFn: ({ signal }) => getPokemonById(id!, signal),
    enabled: id != null,
  })
}
