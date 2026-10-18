import { ArrowLeft } from 'lucide-react'
import { Link, useLocation, useNavigate, useParams } from 'react-router'

import { ApiError } from '@/api/api-error'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { useAuth } from '@/features/auth/useAuth'
import { AbilitiesPanel } from '@/features/pokemon/AbilitiesPanel'
import { CollectionDetailsCard } from '@/features/pokemon/CollectionDetailsCard'
import { DetailHero } from '@/features/pokemon/DetailHero'
import { EvolutionChain } from '@/features/pokemon/EvolutionChain'
import { parsePokemonId } from '@/features/pokemon/parse-pokemon-id'
import { StatsPanel } from '@/features/pokemon/StatsPanel'
import { useCatalogPokemon } from '@/features/pokemon/useCatalogPokemon'
import { useLocalPokemonStatus } from '@/features/pokemon/useLocalPokemonStatus'
import { sanitizeReturnPath } from '@/lib/return-path'

export type PokemonDetailPageProps = {
  onAddToCollection?: () => void
}

type FromState = {
  from?: { pathname?: string; search?: string }
}

function breadcrumbFrom(state: unknown): {
  label: string
  href: string
} {
  const from = (state as FromState | null)?.from
  const pathname = from?.pathname ?? '/'
  const search = from?.search ?? ''
  if (pathname.startsWith('/collection')) {
    return {
      label: 'Collection',
      href: `${pathname}${search}`,
    }
  }
  return {
    label: 'Catalog',
    href: `${pathname === '/' ? '/' : pathname}${search}`,
  }
}

export function PokemonDetailPage({
  onAddToCollection,
}: PokemonDetailPageProps = {}) {
  const { id: rawId } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const { isAuthenticated } = useAuth()
  const id = parsePokemonId(rawId)

  const catalog = useCatalogPokemon(id)
  const localStatus = useLocalPokemonStatus(id)

  const crumb = breadcrumbFrom(location.state)

  const handleAddToCollection =
    onAddToCollection ??
    (() => {
      if (!isAuthenticated) {
        const returnTo = sanitizeReturnPath(
          `${location.pathname}${location.search}`,
        )
        void navigate('/login', { state: { from: returnTo } })
        return
      }
      // Import dialogs land in HU-03.
    })

  if (id == null) {
    return (
      <div className="mx-auto max-w-6xl px-6 py-10">
        <p className="text-title font-semibold text-foreground">
          Pokémon not found
        </p>
        <p className="mt-2 text-body text-muted-foreground">
          This national Pokédex number is not valid.
        </p>
        <Button asChild className="mt-6">
          <Link to="/">Back to catalog</Link>
        </Button>
      </div>
    )
  }

  const catalogStatus =
    catalog.error instanceof ApiError ? catalog.error.status : null
  const catalogNotFound = catalogStatus === 404
  const catalogUnavailable = catalogStatus === 502
  const catalogNetwork = catalogStatus === 0

  if (catalog.isPending) {
    return (
      <div className="mx-auto max-w-6xl px-6 py-10">
        <p className="font-mono text-label tracking-wide text-muted-foreground uppercase">
          Loading species record…
        </p>
      </div>
    )
  }

  if (catalogNotFound) {
    return (
      <div className="mx-auto max-w-6xl px-6 py-10">
        <p className="text-title font-semibold text-foreground">
          Pokémon not found
        </p>
        <p className="mt-2 text-body text-muted-foreground">
          This species is not in the catalog.
        </p>
        <Button asChild className="mt-6">
          <Link to="/">Back to catalog</Link>
        </Button>
      </div>
    )
  }

  if (catalogUnavailable) {
    return (
      <div className="mx-auto max-w-6xl px-6 py-10">
        <Alert className="rounded-md border-danger-border bg-danger-muted">
          <AlertTitle>Catalog unavailable</AlertTitle>
          <AlertDescription>
            The catalog service is temporarily unavailable. Try again in a
            moment.
          </AlertDescription>
          <Button
            type="button"
            className="mt-4"
            onClick={() => void catalog.refetch()}
          >
            Retry
          </Button>
        </Alert>
      </div>
    )
  }

  if (catalogNetwork) {
    return (
      <div className="mx-auto max-w-6xl px-6 py-10">
        <Alert className="rounded-md">
          <AlertTitle>Connection error</AlertTitle>
          <AlertDescription>
            We couldn’t connect to the server. Check your connection and try
            again.
          </AlertDescription>
          <Button
            type="button"
            className="mt-4"
            onClick={() => void catalog.refetch()}
          >
            Retry
          </Button>
        </Alert>
      </div>
    )
  }

  if (catalog.isError || !catalog.data) {
    return (
      <div className="mx-auto max-w-6xl px-6 py-10">
        <Alert className="rounded-md" variant="destructive">
          <AlertTitle>Something went wrong</AlertTitle>
          <AlertDescription>
            {catalog.error instanceof ApiError
              ? catalog.error.detail
              : 'Unexpected error. Try again.'}
          </AlertDescription>
          <Button
            type="button"
            className="mt-4"
            onClick={() => void catalog.refetch()}
          >
            Retry
          </Button>
        </Alert>
      </div>
    )
  }

  const pokemon = catalog.data

  return (
    <div className="mx-auto max-w-6xl px-6 py-6 md:px-14 md:py-0">
      <div className="flex h-16 items-center justify-between gap-4 md:h-[76px]">
        <nav
          aria-label="Breadcrumb"
          className="flex items-center gap-2 text-body-sm"
        >
          <ArrowLeft className="size-4 shrink-0 text-foreground" aria-hidden />
          <Link
            to={crumb.href}
            className="font-medium text-foreground hover:underline"
          >
            {crumb.label}
          </Link>
          <span className="text-nav-muted" aria-hidden>
            /
          </span>
          <span className="text-muted-foreground">{pokemon.name}</span>
        </nav>
        <p className="hidden font-mono text-label tracking-wide text-muted-foreground uppercase md:block">
          Pokédex / Species record
        </p>
      </div>

      <div className="flex flex-col gap-7 pb-8 md:gap-8 md:pb-10">
        <DetailHero
          pokemon={pokemon}
          saved={localStatus.saved}
          statusUnavailable={localStatus.statusUnavailable}
          onAddToCollection={handleAddToCollection}
        />

        {localStatus.saved ? (
          <CollectionDetailsCard local={localStatus.saved} />
        ) : null}

        <div className="grid gap-6 md:grid-cols-2">
          <StatsPanel stats={pokemon.stats} />
          <AbilitiesPanel abilities={pokemon.abilities} />
        </div>

        <EvolutionChain root={pokemon.evolution} />
      </div>
    </div>
  )
}
