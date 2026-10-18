import { ArrowLeft, FileSearch } from 'lucide-react'
import { useState } from 'react'
import { Link, useParams } from 'react-router'

import { ApiError } from '@/api/api-error'
import { PageHeading } from '@/components/page-heading'
import { Button } from '@/components/ui/button'
import { authLinkWithRedirect } from '@/features/auth/auth-redirect'
import { useAuth } from '@/features/auth/useAuth'
import { EditForm } from '@/features/collection/EditForm'
import { useLocalPokemon } from '@/features/collection/useLocalPokemon'
import { TypeBadge } from '@/features/pokemon/type-badge'
import { parsePokemonId } from '@/features/pokemon/parse-pokemon-id'
import { formatNationalNumber } from '@/lib/format'

function NotInCollection({ id }: { id: number | null }) {
  return (
    <div className="flex flex-col items-center gap-5 rounded-xl border border-border bg-card p-10 text-center">
      <div className="flex size-14 items-center justify-center rounded-xl bg-surface">
        <FileSearch className="size-7 text-muted-foreground" aria-hidden />
      </div>
      <p className="text-display font-semibold text-foreground">
        Not in the collection
      </p>
      <p className="max-w-md text-[0.8125rem] leading-[1.6] text-muted-foreground">
        This Pokémon is not saved in the shared collection. Open the catalog
        record or return to the collection list.
      </p>
      <div className="flex w-full max-w-[240px] flex-col gap-2.5">
        {id != null ? (
          <Button asChild className="h-12 rounded-[10px]">
            <Link to={`/pokemon/${id}`}>View in catalog</Link>
          </Button>
        ) : null}
        <Button asChild variant="outline" className="h-12 rounded-[10px]">
          <Link to="/collection">Back to collection</Link>
        </Button>
      </div>
    </div>
  )
}

export function CollectionEditPage() {
  const { id: rawId } = useParams()
  const id = parsePokemonId(rawId)
  const { isAuthenticated } = useAuth()
  const query = useLocalPokemon(id)
  const [forcedNotFound, setForcedNotFound] = useState(false)

  const loginHref = authLinkWithRedirect(
    '/login',
    id != null ? `/collection/${id}` : '/collection',
  )

  if (id == null) {
    return (
      <div className="mx-auto max-w-6xl px-6 py-10">
        <NotInCollection id={null} />
      </div>
    )
  }

  const notFound =
    forcedNotFound ||
    (query.error instanceof ApiError && query.error.status === 404)

  if (query.isPending) {
    return (
      <div className="mx-auto max-w-6xl px-6 py-10">
        <p className="font-mono text-label tracking-wide text-muted-foreground uppercase">
          Loading collection record…
        </p>
      </div>
    )
  }

  if (notFound) {
    return (
      <div className="mx-auto max-w-6xl px-6 py-10">
        <NotInCollection id={id} />
      </div>
    )
  }

  if (query.isError || !query.data) {
    return (
      <div className="mx-auto max-w-6xl px-6 py-10">
        <p className="text-title font-semibold text-foreground">
          Something went wrong
        </p>
        <p className="mt-2 text-body text-muted-foreground">
          {query.error instanceof ApiError
            ? query.error.detail
            : 'Unexpected error. Try again.'}
        </p>
        <Button
          type="button"
          className="mt-6"
          onClick={() => void query.refetch()}
        >
          Retry
        </Button>
      </div>
    )
  }

  const pokemon = query.data

  return (
    <div className="mx-auto max-w-6xl px-6 py-6 md:px-14 md:py-0">
      <div className="flex h-16 items-center justify-between gap-4 md:h-[76px]">
        <nav
          aria-label="Breadcrumb"
          className="flex flex-wrap items-center gap-2 text-body-sm"
        >
          <ArrowLeft className="size-4 shrink-0 text-foreground" aria-hidden />
          <Link
            to="/collection"
            className="font-medium text-foreground hover:underline"
          >
            Collection
          </Link>
          <span className="text-nav-muted" aria-hidden>
            /
          </span>
          <Link
            to={`/pokemon/${pokemon.nationalNumber}`}
            className="text-muted-foreground hover:underline"
          >
            {pokemon.name}
          </Link>
          <span className="text-nav-muted" aria-hidden>
            /
          </span>
          <span className="text-muted-foreground">Edit</span>
        </nav>
        <p className="hidden font-mono text-label tracking-wide text-muted-foreground uppercase md:block">
          Pokéfolio / Shared archive
        </p>
      </div>

      <div className="flex flex-col gap-8 pb-10">
        <div className="space-y-3">
          <div className="flex items-center gap-2">
            <span className="h-0.5 w-4 bg-primary" aria-hidden />
            <p className="font-mono text-label tracking-wide text-muted-foreground uppercase">
              Collection / Edit record
            </p>
          </div>
          <PageHeading>Edit Pokémon</PageHeading>
          <p className="text-[0.9375rem] leading-[1.6] text-muted-foreground">
            Update the personal details saved in the shared collection.
          </p>
        </div>

        <div className="flex flex-col gap-8 lg:flex-row lg:items-start">
          <aside className="flex w-full flex-col gap-6 rounded-xl border border-border bg-surface p-6 lg:w-[400px] lg:shrink-0 lg:p-7">
            <div className="flex items-center justify-between font-mono text-[0.5625rem] tracking-wide text-muted-foreground uppercase">
              <p>Official Pokédex record</p>
              <p>Read only</p>
            </div>
            <div className="relative mx-auto aspect-square w-full max-w-[344px] overflow-hidden rounded-lg bg-surface">
              <img
                src={pokemon.imageUrl}
                alt={pokemon.name}
                width={160}
                height={160}
                className="absolute top-1/2 left-1/2 size-[45%] -translate-x-1/2 -translate-y-1/2 object-contain"
              />
            </div>
            <div className="space-y-2">
              <p className="font-mono text-[0.6875rem] text-muted-foreground">
                {formatNationalNumber(pokemon.nationalNumber)} · National
                Pokédex
              </p>
              <p className="text-[1.875rem] leading-tight font-semibold text-foreground">
                {pokemon.name}
              </p>
              <p className="text-[0.8125rem] text-muted-foreground">
                {pokemon.category}
              </p>
            </div>
            <ul className="flex flex-wrap gap-2">
              {pokemon.types.map((type) => (
                <li key={type}>
                  <TypeBadge type={type} />
                </li>
              ))}
            </ul>
            <p className="text-caption leading-[1.6] text-muted-foreground">
              Official data via PokéAPI. Changes only affect collection
              details.
            </p>
          </aside>

          <div className="min-w-0 flex-1">
            <EditForm
              key={`${pokemon.nationalNumber}-${pokemon.version}-${pokemon.syncedAt}`}
              pokemon={pokemon}
              readOnly={!isAuthenticated}
              loginHref={loginHref}
              onNotFound={() => setForcedNotFound(true)}
              onReload={async () => {
                const result = await query.refetch()
                if (!result.data) {
                  throw result.error ?? new Error('Reload failed')
                }
                return result.data
              }}
            />
          </div>
        </div>
      </div>
    </div>
  )
}
