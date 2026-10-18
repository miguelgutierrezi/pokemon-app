import type { ApiClient } from '@/api/client'
import type { PageQuery, PokemonDetail, PokemonPage } from '@/api/types'

function pageQuery(query: PageQuery): string {
  const params = new URLSearchParams({
    page: String(query.page),
    size: String(query.size),
  })
  return params.toString()
}

export function getPokemonPage(
  client: ApiClient,
  query: PageQuery,
  signal?: AbortSignal,
): Promise<PokemonPage> {
  return client.request<PokemonPage>(
    `/api/v1/pokemon?${pageQuery(query)}`,
    { signal },
  )
}

export function getPokemon(
  client: ApiClient,
  id: number,
  signal?: AbortSignal,
): Promise<PokemonDetail> {
  return client.request<PokemonDetail>(`/api/v1/pokemon/${id}`, { signal })
}
