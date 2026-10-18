import { Link } from 'react-router'

import type { LocalPokemon } from '@/api/types'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { COLLECTION_ROW_GRID } from '@/features/collection/collection-layout'
import { TypeBadge } from '@/features/pokemon/type-badge'
import { formatNationalNumber } from '@/lib/format'
import { cn } from '@/lib/utils'

const IMAGE_SIZE = 80

function displayOrDash(value: string | null | undefined): string {
  if (value == null || value.trim() === '') {
    return '—'
  }
  return value
}

export type CollectionEntryCardProps = {
  pokemon: LocalPokemon
  editHref: string
  onRemove: () => void
}

export function CollectionEntryCard({
  pokemon,
  editHref,
  onRemove,
}: CollectionEntryCardProps) {
  const tagsEmpty = pokemon.tags.length === 0

  return (
    <article
      aria-label={pokemon.name}
      className={cn(
        'flex flex-col gap-5 border-t border-border p-5',
        COLLECTION_ROW_GRID,
      )}
    >
      <div className="flex min-w-0 items-center gap-3 md:gap-3">
        <div className="order-2 size-[88px] shrink-0 overflow-hidden rounded-[10px] bg-surface md:order-1 md:size-16 lg:size-[4.5rem]">
          <img
            src={pokemon.imageUrl}
            alt={pokemon.name}
            width={IMAGE_SIZE}
            height={IMAGE_SIZE}
            loading="lazy"
            className="size-full object-contain p-1.5"
          />
        </div>
        <div className="order-1 min-w-0 flex-1 space-y-1.5 md:order-2">
          <p className="font-mono text-[0.6875rem] text-muted-foreground">
            {formatNationalNumber(pokemon.nationalNumber)}
          </p>
          <h2 className="truncate text-[1.3125rem] leading-tight font-semibold text-foreground md:text-base lg:text-lg">
            <Link
              to={`/pokemon/${pokemon.nationalNumber}`}
              className="hover:underline"
            >
              {pokemon.name}
            </Link>
          </h2>
          <p className="truncate text-caption text-muted-foreground">
            {pokemon.category}
          </p>
          <ul className="flex flex-wrap gap-1">
            {pokemon.types.map((type) => (
              <li key={type}>
                <TypeBadge type={type} />
              </li>
            ))}
          </ul>
        </div>
      </div>

      <div className="flex flex-col gap-4 border-t border-border pt-4 md:contents md:border-0 md:pt-0">
        <p className="font-mono text-[0.5625rem] tracking-wide text-primary uppercase md:hidden">
          Collection details
        </p>

        <div className="min-w-0 space-y-1.5">
          <p className="text-[0.6875rem] text-muted-foreground md:sr-only">
            Localized name
          </p>
          <p className="truncate text-body font-medium text-foreground">
            {displayOrDash(pokemon.localizedName)}
          </p>
        </div>

        <div className="min-w-0 space-y-1.5">
          <p className="text-[0.6875rem] text-muted-foreground md:sr-only">
            Location
          </p>
          <p className="line-clamp-2 text-[0.8125rem] leading-[1.5] text-foreground">
            {displayOrDash(pokemon.location)}
          </p>
        </div>

        <div className="min-w-0 space-y-2">
          <p className="text-[0.6875rem] text-muted-foreground md:sr-only">
            Tags
          </p>
          {tagsEmpty ? (
            <p className="text-body text-foreground">—</p>
          ) : (
            <ul className="flex flex-wrap gap-1">
              {pokemon.tags.map((tag) => (
                <li key={tag}>
                  <Badge
                    variant="outline"
                    className="rounded-md px-2 py-1 text-[0.6875rem] font-normal"
                  >
                    {tag}
                  </Badge>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>

      <div className="flex w-full shrink-0 items-center justify-between gap-3 border-t border-border pt-2 md:w-auto md:flex-col md:items-end md:justify-center md:gap-2 md:border-0 md:pt-0">
        <Link
          to={`/pokemon/${pokemon.nationalNumber}`}
          className="whitespace-nowrap text-body-sm font-semibold text-primary hover:underline"
        >
          View details →
        </Link>
        <div className="flex shrink-0 items-center gap-3">
          <Link
            to={editHref}
            className="whitespace-nowrap text-body-sm font-medium text-foreground hover:underline"
          >
            Edit
          </Link>
          <span className="hidden h-3.5 w-px bg-border md:block" aria-hidden />
          <Button
            type="button"
            variant="link"
            className="h-auto px-0 text-body-sm font-medium whitespace-nowrap text-primary"
            onClick={onRemove}
          >
            Remove
          </Button>
        </div>
      </div>
    </article>
  )
}
