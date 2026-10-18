import type { PokemonSummary } from '@/api/types'
import { PokemonCard } from '@/features/catalog/PokemonCard'

export function CatalogGrid({ items }: { items: PokemonSummary[] }) {
  return (
    <ul className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {items.map((pokemon) => (
        <li key={pokemon.nationalNumber}>
          <PokemonCard pokemon={pokemon} />
        </li>
      ))}
    </ul>
  )
}
