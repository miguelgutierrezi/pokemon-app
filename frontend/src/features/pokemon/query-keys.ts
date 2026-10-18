export function catalogPokemonQueryKey(id: number) {
  return ['pokemon', 'detail', id] as const
}

export function localPokemonStatusQueryKey(id: number) {
  return ['collection', 'status', id] as const
}
