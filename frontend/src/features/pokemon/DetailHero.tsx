import { BookmarkPlus, CheckCircle2 } from 'lucide-react'
import type { Ref } from 'react'
import { Link } from 'react-router'

import type { LocalPokemon, PokemonDetail } from '@/api/types'
import { Button } from '@/components/ui/button'
import { TypeBadge } from '@/features/pokemon/type-badge'
import {
  formatFlavorSource,
  formatGeneration,
  formatHeightM,
  formatNationalNumber,
  formatWeightKg,
} from '@/lib/format'
import { cn } from '@/lib/utils'

const ARTWORK_SIZE = 254

export type DetailHeroProps = {
  pokemon: PokemonDetail
  saved: LocalPokemon | null
  statusUnavailable: boolean
  onAddToCollection: () => void
  addButtonRef?: Ref<HTMLButtonElement>
}

export function DetailHero({
  pokemon,
  saved,
  statusUnavailable,
  onAddToCollection,
  addButtonRef,
}: DetailHeroProps) {
  const specimen = formatNationalNumber(pokemon.nationalNumber).slice(1)

  return (
    <div className="flex flex-col gap-6 md:flex-row md:items-center md:gap-12">
      <div className="relative order-2 mx-auto aspect-square w-full max-w-[520px] overflow-hidden rounded-lg border border-border bg-surface md:order-1 md:mx-0 md:h-[448px] md:max-w-none md:w-[520px]">
        <div
          className="pointer-events-none absolute inset-x-6 top-1/2 h-px -translate-y-1/2 bg-border"
          aria-hidden
        />
        <div
          className="pointer-events-none absolute inset-y-6 left-1/2 w-px -translate-x-1/2 bg-border"
          aria-hidden
        />
        <div
          className="pointer-events-none absolute inset-[12%] rounded-full border border-border/80"
          aria-hidden
        />
        <div
          className="pointer-events-none absolute inset-[18%] rounded-full border border-border/60"
          aria-hidden
        />
        <p className="absolute top-4 left-5 font-mono text-[0.5625rem] tracking-wide text-muted-foreground uppercase">
          Specimen / {specimen}
        </p>
        <img
          src={pokemon.imageUrl}
          alt={pokemon.name}
          width={ARTWORK_SIZE}
          height={ARTWORK_SIZE}
          loading="eager"
          className="absolute top-1/2 left-1/2 size-[min(63%,254px)] -translate-x-1/2 -translate-y-1/2 object-contain"
        />
        <p className="absolute bottom-4 left-5 font-mono text-[0.5625rem] tracking-wide text-muted-foreground uppercase">
          Official artwork
        </p>
        <span
          className="absolute right-5 bottom-4 font-mono text-title text-primary"
          aria-hidden
        >
          +
        </span>
      </div>

      <div className="order-1 flex min-w-0 flex-1 flex-col gap-6 md:order-2">
        <div className="space-y-3">
          <div className="flex items-center gap-2">
            <span className="h-0.5 w-4 bg-primary" aria-hidden />
            <p className="font-mono text-label tracking-wide text-muted-foreground uppercase">
              {formatNationalNumber(pokemon.nationalNumber)} · National Pokédex
            </p>
          </div>
          <h1 className="text-[2.25rem] leading-tight font-semibold tracking-tight text-foreground md:text-hero md:leading-[1.1]">
            {pokemon.name}
          </h1>
          <div className="flex flex-wrap items-center justify-between gap-3">
            <p className="text-body text-muted-foreground md:text-base">
              {pokemon.category}
            </p>
            <ul className="flex flex-wrap gap-2">
              {pokemon.types.map((type) => (
                <li key={type}>
                  <TypeBadge type={type} />
                </li>
              ))}
            </ul>
          </div>
        </div>

        <div className="order-3 space-y-2 md:order-0">
          {saved ? (
            <>
              <div className="flex items-center justify-between gap-3 rounded-md border border-danger-border bg-danger-muted px-4 py-3">
                <div className="flex items-center gap-2.5">
                  <CheckCircle2 className="size-5 text-primary" aria-hidden />
                  <p className="text-body font-semibold text-foreground">
                    Already in the collection
                  </p>
                </div>
                <Button
                  asChild
                  variant="link"
                  className="h-auto px-0 text-body font-semibold"
                >
                  <Link to={`/collection/${pokemon.nationalNumber}`}>Edit</Link>
                </Button>
              </div>
              <p className="text-caption text-muted-foreground">
                Saved in the shared collection
              </p>
            </>
          ) : (
            <>
              <Button
                ref={addButtonRef}
                type="button"
                className="h-[52px] w-full rounded-md text-body font-semibold"
                disabled={statusUnavailable}
                onClick={onAddToCollection}
              >
                <BookmarkPlus className="size-[1.125rem]" aria-hidden />
                Add to collection
              </Button>
              <p className={cn('text-caption text-muted-foreground')}>
                {statusUnavailable
                  ? 'Collection status unavailable. You can still browse this record.'
                  : 'Keep this Pokémon and add your own collection details.'}
              </p>
            </>
          )}
        </div>

        <div className="order-4 grid grid-cols-3 gap-6 border-y border-border py-[18px] md:order-0">
          <div className="space-y-2">
            <p className="font-mono text-label tracking-wide text-muted-foreground uppercase">
              Height
            </p>
            <p className="text-body font-medium text-foreground">
              {formatHeightM(pokemon.heightM)}
            </p>
          </div>
          <div className="space-y-2">
            <p className="font-mono text-label tracking-wide text-muted-foreground uppercase">
              Weight
            </p>
            <p className="text-body font-medium text-foreground">
              {formatWeightKg(pokemon.weightKg)}
            </p>
          </div>
          <div className="space-y-2">
            <p className="font-mono text-label tracking-wide text-muted-foreground uppercase">
              Debut
            </p>
            <p className="text-body font-medium text-foreground">
              {formatGeneration(pokemon.generation)}
            </p>
          </div>
        </div>

        <div className="order-5 space-y-2.5 md:order-0">
          <p className="font-mono text-label tracking-wide text-muted-foreground uppercase">
            Field notes / Pokédex entry
          </p>
          <p className="text-[0.9375rem] leading-[1.65] text-foreground">
            {pokemon.flavorText.text}
          </p>
          <p className="text-caption text-muted-foreground">
            {formatFlavorSource(pokemon.flavorText.version)}
          </p>
        </div>
      </div>
    </div>
  )
}
