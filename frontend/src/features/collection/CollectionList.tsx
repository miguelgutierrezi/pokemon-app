import type { LocalPokemon } from '@/api/types'
import { COLLECTION_ROW_GRID } from '@/features/collection/collection-layout'
import { CollectionEntryCard } from '@/features/collection/CollectionEntryCard'

export type CollectionListProps = {
  items: LocalPokemon[]
  editHrefFor: (pokemon: LocalPokemon) => string
  onRemove: (pokemon: LocalPokemon) => void
}

export function CollectionList({
  items,
  editHrefFor,
  onRemove,
}: CollectionListProps) {
  return (
    <div className="overflow-x-auto rounded-xl border border-border">
      <div className="min-w-[40rem] bg-card md:min-w-0">
        <div
          className={`hidden px-5 pt-[18px] pb-4 font-mono text-label uppercase ${COLLECTION_ROW_GRID}`}
        >
          <p className="text-muted-foreground">Official Pokédex record</p>
          <p className="col-span-3 text-primary">Collection details</p>
          <p className="sr-only">Actions</p>
        </div>
        <div
          className={`hidden border-t border-border bg-surface px-5 py-3 text-[0.75rem] font-medium text-muted-foreground ${COLLECTION_ROW_GRID}`}
        >
          <p>Pokémon</p>
          <p>Localized name</p>
          <p>Location</p>
          <p>Tags</p>
          <p className="text-right font-normal">Actions</p>
        </div>
        <ul className="flex flex-col">
          {items.map((pokemon) => (
            <li key={pokemon.nationalNumber}>
              <CollectionEntryCard
                pokemon={pokemon}
                editHref={editHrefFor(pokemon)}
                onRemove={() => onRemove(pokemon)}
              />
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}
