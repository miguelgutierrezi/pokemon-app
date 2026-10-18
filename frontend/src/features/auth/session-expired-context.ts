/**
 * Human-readable context label for the session-expired dialog.
 * Prefer "Screen / Entity" when a name is known; otherwise the route.
 */
export function buildSessionExpiredContext(
  pathname: string,
  search: string,
  entityName?: string | null,
): string {
  const pokemonMatch = pathname.match(/^\/pokemon\/(\d+)\/?$/)
  if (pokemonMatch) {
    return entityName?.trim()
      ? `Pokémon details / ${entityName.trim()}`
      : 'Pokémon details'
  }

  const editMatch = pathname.match(/^\/collection\/(\d+)\/?$/)
  if (editMatch) {
    return entityName?.trim()
      ? `Edit Pokémon / ${entityName.trim()}`
      : 'Edit Pokémon'
  }

  if (pathname === '/collection') {
    return 'Collection'
  }

  if (pathname === '/') {
    return 'Pokémon catalog'
  }

  const route = `${pathname}${search}`
  return route.length > 0 ? route : '/'
}

export function sessionExpiredEntityId(pathname: string): number | null {
  const match = pathname.match(/^\/(?:pokemon|collection)\/(\d+)\/?$/)
  if (!match) {
    return null
  }
  const id = Number(match[1])
  return Number.isInteger(id) && id > 0 ? id : null
}
