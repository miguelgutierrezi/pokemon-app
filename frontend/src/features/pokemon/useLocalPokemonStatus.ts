import { useQuery } from '@tanstack/react-query'

import { ApiError } from '@/api/api-error'
import { getLocalPokemonById } from '@/api/collection'
import { localPokemonStatusQueryKey } from '@/features/pokemon/query-keys'

export function useLocalPokemonStatus(id: number | null) {
  const query = useQuery({
    queryKey: localPokemonStatusQueryKey(id ?? 0),
    queryFn: ({ signal }) => getLocalPokemonById(id!, signal),
    enabled: id != null,
    // 404 is the not-saved signal; 502/0 surface as degraded CTA — no retries.
    retry: false,
  })

  const notFound =
    query.error instanceof ApiError && query.error.status === 404
  const statusUnavailable =
    query.isError &&
    query.error instanceof ApiError &&
    (query.error.status === 0 || query.error.status === 502)
  const saved = query.isSuccess ? query.data : null

  return {
    ...query,
    saved,
    notFound,
    statusUnavailable,
    isNotSaved: notFound,
  }
}
