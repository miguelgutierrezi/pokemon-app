import type { ApiClient } from '@/api/client'
import type {
  ImportPokemonRequest,
  LocalPokemon,
  LocalPokemonPage,
  PageQuery,
  UpdateLocalPokemonRequest,
} from '@/api/types'

function pageQuery(query: PageQuery): string {
  const params = new URLSearchParams({
    page: String(query.page),
    size: String(query.size),
  })
  return params.toString()
}

export function getCollection(
  client: ApiClient,
  query: PageQuery,
  signal?: AbortSignal,
): Promise<LocalPokemonPage> {
  return client.request<LocalPokemonPage>(
    `/api/v1/collection?${pageQuery(query)}`,
    { signal },
  )
}

export function getLocalPokemon(
  client: ApiClient,
  id: number,
  signal?: AbortSignal,
): Promise<LocalPokemon> {
  return client.request<LocalPokemon>(`/api/v1/collection/${id}`, { signal })
}

export function importPokemon(
  client: ApiClient,
  body: ImportPokemonRequest,
  signal?: AbortSignal,
): Promise<LocalPokemon> {
  return client.request<LocalPokemon>('/api/v1/collection', {
    method: 'POST',
    body,
    auth: true,
    signal,
  })
}

export function updateLocalPokemon(
  client: ApiClient,
  id: number,
  body: UpdateLocalPokemonRequest,
  signal?: AbortSignal,
): Promise<LocalPokemon> {
  return client.request<LocalPokemon>(`/api/v1/collection/${id}`, {
    method: 'PUT',
    body,
    auth: true,
    signal,
  })
}

export function deleteLocalPokemon(
  client: ApiClient,
  id: number,
  signal?: AbortSignal,
): Promise<void> {
  return client.request<void>(`/api/v1/collection/${id}`, {
    method: 'DELETE',
    auth: true,
    signal,
  })
}
