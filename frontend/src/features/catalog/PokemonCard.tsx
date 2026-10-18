import { ArrowRight } from 'lucide-react'
import { Link, useLocation } from 'react-router'

import type { PokemonSummary } from '@/api/types'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent } from '@/components/ui/card'
import {
  formatNationalNumber,
  formatSlug,
  formatWeightKg,
} from '@/lib/format'

/** Figma sprite plate is 116×116. */
const SPRITE_SIZE = 116

export function PokemonCard({ pokemon }: { pokemon: PokemonSummary }) {
  const location = useLocation()

  return (
    <Card className="rounded-md border-border py-0 shadow-none transition-colors hover:border-primary/40">
      <Link
        to={`/pokemon/${pokemon.nationalNumber}`}
        state={{
          from: { pathname: location.pathname, search: location.search },
        }}
        className="block h-full outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50"
        aria-label={pokemon.name}
      >
        <CardContent className="flex h-full flex-col gap-4 p-5">
          <div className="flex items-start justify-between gap-3">
            <div className="min-w-0 space-y-1">
              <p className="font-mono text-label tracking-wide text-muted-foreground">
                {formatNationalNumber(pokemon.nationalNumber)}
              </p>
              <h2 className="text-title font-semibold text-foreground">
                {pokemon.name}
              </h2>
              <p className="font-mono text-label tracking-wide text-muted-foreground uppercase">
                National Pokédex
              </p>
            </div>
            <div className="flex size-[7.25rem] shrink-0 items-center justify-center rounded-md bg-surface">
              <img
                src={pokemon.spriteUrl}
                alt={pokemon.name}
                width={SPRITE_SIZE}
                height={SPRITE_SIZE}
                loading="lazy"
                className="size-[7.25rem] object-contain"
                style={{ imageRendering: 'pixelated' }}
              />
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <div>
              <p className="font-mono text-label tracking-wide text-muted-foreground uppercase">
                Category
              </p>
              <p className="mt-1 text-body-sm text-foreground">
                {pokemon.category}
              </p>
            </div>
            <div>
              <p className="font-mono text-label tracking-wide text-muted-foreground uppercase">
                Weight
              </p>
              <p className="mt-1 text-body-sm text-foreground">
                {formatWeightKg(pokemon.weightKg)}
              </p>
            </div>
          </div>

          <div>
            <p className="font-mono text-label tracking-wide text-muted-foreground uppercase">
              Abilities
            </p>
            <ul className="mt-2 flex flex-wrap gap-2">
              {pokemon.abilities.map((ability) => (
                <li key={`${ability.name}-${ability.hidden}`}>
                  <Badge
                    variant="secondary"
                    className="rounded-sm bg-muted px-2.5 py-1 text-caption font-normal text-foreground"
                  >
                    {formatSlug(ability.name)}
                    {ability.hidden ? (
                      <span className="ml-1 font-mono text-label text-muted-foreground">
                        <span aria-hidden="true">H</span>
                        <span className="sr-only">Hidden ability</span>
                      </span>
                    ) : null}
                  </Badge>
                </li>
              ))}
            </ul>
          </div>

          <div className="mt-auto flex items-center justify-between pt-1 text-body-sm font-semibold text-primary">
            <span>View details</span>
            <ArrowRight className="size-4" aria-hidden />
          </div>
        </CardContent>
      </Link>
    </Card>
  )
}
