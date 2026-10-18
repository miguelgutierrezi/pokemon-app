import { WifiOff } from 'lucide-react'
import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'

import { ApiError } from '@/api/api-error'
import type { LocalPokemon } from '@/api/types'
import { PageHeading } from '@/components/page-heading'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { authLinkWithRedirect } from '@/features/auth/auth-redirect'
import { useAuth } from '@/features/auth/useAuth'
import { CatalogPagination } from '@/features/catalog/CatalogPagination'
import { CollectionList } from '@/features/collection/CollectionList'
import { CollectionSkeleton } from '@/features/collection/CollectionSkeleton'
import { EmptyCollection } from '@/features/collection/EmptyCollection'
import { RemoveDialog } from '@/features/collection/RemoveDialog'
import { useCollectionPage } from '@/features/collection/useCollectionPage'
import { useDeleteLocalPokemon } from '@/features/collection/useDeleteLocalPokemon'

export function CollectionPage() {
  const location = useLocation()
  const navigate = useNavigate()
  const { isAuthenticated } = useAuth()
  const deleteMutation = useDeleteLocalPokemon()
  const [removeTarget, setRemoveTarget] = useState<LocalPokemon | null>(null)
  const {
    page,
    pageInvalid,
    beyondLast,
    data,
    error,
    isPending,
    isFetching,
    isError,
    refetch,
    isPlaceholderData,
  } = useCollectionPage()

  const returnPath = `${location.pathname}${location.search}`
  const loginHref = authLinkWithRedirect('/login', returnPath)

  const editHrefFor = (pokemon: LocalPokemon) =>
    isAuthenticated
      ? `/collection/${pokemon.nationalNumber}`
      : loginHref

  const handleRemove = (pokemon: LocalPokemon) => {
    if (!isAuthenticated) {
      void navigate(loginHref)
      return
    }
    setRemoveTarget(pokemon)
  }

  async function handleConfirmRemove() {
    if (removeTarget == null) {
      return
    }
    try {
      await deleteMutation.mutateAsync({
        id: removeTarget.nationalNumber,
        name: removeTarget.name,
      })
      setRemoveTarget(null)
      void navigate('/collection')
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) {
        setRemoveTarget(null)
        void navigate('/collection')
      }
      if (error instanceof ApiError && error.status === 401) {
        setRemoveTarget(null)
      }
    }
  }

  if (pageInvalid) {
    return (
      <div className="mx-auto max-w-6xl px-6 py-10">
        <CollectionSkeleton />
      </div>
    )
  }

  const errorStatus = error instanceof ApiError ? error.status : null
  const isNetworkError = errorStatus === 0
  const isUnavailable = errorStatus === 502
  const showGenericError =
    isError && !beyondLast && !isUnavailable && !isNetworkError
  const isEmpty =
    data != null &&
    !beyondLast &&
    !isError &&
    data.totalElements === 0 &&
    data.content.length === 0

  return (
    <>
    <div className="mx-auto max-w-6xl px-6 py-8 md:px-14 md:py-10">
      <div className="mb-8 flex flex-col gap-6 border-b border-border pb-8 md:flex-row md:items-end md:justify-between">
        <div className="max-w-3xl space-y-3.5">
          <div className="flex items-center gap-2">
            <span className="h-0.5 w-4 bg-primary" aria-hidden />
            <p className="font-mono text-label tracking-wide text-muted-foreground uppercase">
              Pokéfolio / Shared archive
            </p>
          </div>
          <PageHeading>Collection</PageHeading>
          <p className="text-[0.9375rem] leading-[1.6] text-muted-foreground">
            Pokémon saved in the shared collection, with localized names,
            locations and tags.
          </p>
          <p className="text-[0.6875rem] leading-[1.5] text-muted-foreground">
            Shared collection · saved on the server.
          </p>
        </div>
        <div className="flex flex-col gap-2.5 md:items-end">
          {data && !beyondLast && !isEmpty ? (
            <>
              <p className="font-mono text-display text-foreground">
                {data.totalElements.toLocaleString()}
              </p>
              <p className="font-mono text-label tracking-wide text-muted-foreground uppercase">
                Saved Pokémon · all generations
              </p>
            </>
          ) : null}
          <Button asChild variant="outline" className="h-11 rounded-[10px]">
            <Link to="/">Browse Pokémon catalog</Link>
          </Button>
        </div>
      </div>

      {isPending && !data ? <CollectionSkeleton /> : null}

      {isError && isUnavailable ? (
        <Alert className="rounded-xl border-danger-border bg-danger-muted">
          <AlertTitle>Collection unavailable</AlertTitle>
          <AlertDescription>
            The collection service is temporarily unavailable. Try again in a
            moment.
          </AlertDescription>
          <Button type="button" className="mt-4" onClick={() => void refetch()}>
            Retry
          </Button>
        </Alert>
      ) : null}

      {isError && isNetworkError ? (
        <div className="flex flex-col items-center gap-5 rounded-xl border border-border bg-card p-10 text-center">
          <div className="flex size-14 items-center justify-center rounded-xl bg-danger-muted">
            <WifiOff className="size-7 text-primary" aria-hidden />
          </div>
          <p className="text-display font-semibold text-foreground">
            Connection error
          </p>
          <p className="max-w-md text-[0.8125rem] leading-[1.6] text-muted-foreground">
            We couldn’t connect to the server. Check your connection and try
            again. Saved Pokémon in the shared collection haven’t been changed.
          </p>
          <div className="flex w-full max-w-[240px] flex-col gap-2.5">
            <Button
              type="button"
              className="h-12 rounded-[10px]"
              onClick={() => void refetch()}
            >
              Retry
            </Button>
            <Button asChild variant="outline" className="h-12 rounded-[10px]">
              <Link to="/">Browse catalog</Link>
            </Button>
          </div>
        </div>
      ) : null}

      {showGenericError ? (
        <Alert className="rounded-md" variant="destructive">
          <AlertTitle>Something went wrong</AlertTitle>
          <AlertDescription>
            {error instanceof ApiError
              ? error.detail
              : 'Unexpected error. Try again.'}
          </AlertDescription>
          <Button type="button" className="mt-4" onClick={() => void refetch()}>
            Retry
          </Button>
        </Alert>
      ) : null}

      {beyondLast ? (
        <div className="rounded-md border border-border bg-card p-10 text-center">
          <p className="text-title font-semibold text-foreground">
            No records on this page
          </p>
          <p className="mt-2 text-body text-muted-foreground">
            This page is beyond the end of the collection.
          </p>
          <Button asChild className="mt-6">
            <Link to="/collection?page=1">Back to page 1</Link>
          </Button>
        </div>
      ) : null}

      {isEmpty ? <EmptyCollection /> : null}

      {data && !beyondLast && !isError && !isEmpty ? (
        <div
          className={
            isFetching && isPlaceholderData ? 'opacity-80' : undefined
          }
        >
          <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
            <div className="flex flex-wrap items-baseline gap-x-3 gap-y-1">
              <p className="text-body font-semibold text-foreground">
                Saved Pokémon
              </p>
              <p className="font-mono text-caption tracking-wide text-muted-foreground">
                {data.totalElements}{' '}
                {data.totalElements === 1 ? 'record' : 'records'}
              </p>
            </div>
            <p className="font-mono text-label tracking-wide text-muted-foreground uppercase">
              National number ↑
            </p>
          </div>

          <CollectionList
            items={data.content}
            editHrefFor={editHrefFor}
            onRemove={handleRemove}
          />

          <p className="mt-4 text-caption text-muted-foreground">
            Localized names, locations and tags are separate from the official
            Pokédex data.
          </p>

          <CatalogPagination
            page={page}
            totalPages={data.totalPages}
            basePath="/collection"
          />
        </div>
      ) : null}
    </div>

    <RemoveDialog
      open={removeTarget != null}
      pokemon={removeTarget}
      busy={deleteMutation.isPending}
      onOpenChange={(open) => {
        if (!open) {
          setRemoveTarget(null)
        }
      }}
      onConfirm={() => void handleConfirmRemove()}
    />
    </>
  )
}
