/** Valid national Pokédex id for detail routes — positive integer only. */
export function parsePokemonId(raw: string | undefined): number | null {
  if (raw == null || raw.trim() === '') {
    return null
  }
  if (!/^\d+$/.test(raw)) {
    return null
  }
  const value = Number(raw)
  if (!Number.isInteger(value) || value < 1) {
    return null
  }
  return value
}
