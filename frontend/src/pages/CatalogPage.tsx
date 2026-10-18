import { Link } from 'react-router'

import { ApiError } from '@/api/api-error'
import pokeballArchiveDiagram from '@/assets/pokeball-archive-diagram.svg'
import { PageHeading } from '@/components/page-heading'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { CatalogGrid } from '@/features/catalog/CatalogGrid'
import { CatalogPagination } from '@/features/catalog/CatalogPagination'
import { CatalogSkeleton } from '@/features/catalog/CatalogSkeleton'
import { useCatalogPage } from '@/features/catalog/useCatalogPage'
import { formatRecordRange } from '@/lib/format'

export function CatalogPage() {
  const {
    page,
    pageSize,
    pageInvalid,
    beyondLast,
    data,
    error,
    isPending,
    isFetching,
    isError,
    refetch,
    isPlaceholderData,
  } = useCatalogPage()

  if (pageInvalid) {
    return (
      <div className="mx-auto max-w-6xl px-6 py-10">
        <CatalogSkeleton />
      </div>
    )
  }

  const errorStatus = error instanceof ApiError ? error.status : null
  const isNetworkError = errorStatus === 0
  const isUnavailable = errorStatus === 502
  const showGenericError =
    isError && !beyondLast && !isUnavailable && !isNetworkError

  return (
    <div className="mx-auto max-w-6xl px-6 py-10">
      <div className="mb-8 flex flex-col gap-6 border-b border-border pb-8 md:flex-row md:items-center md:justify-between">
        <div className="max-w-3xl space-y-3">
          <div className="flex items-center gap-2">
            <span className="h-0.5 w-4 bg-primary" aria-hidden />
            <p className="font-mono text-label tracking-wide text-muted-foreground uppercase">
              Pokédex / National catalog
            </p>
          </div>
          <PageHeading>The Pokémon catalog</PageHeading>
          <p className="text-body text-muted-foreground">
            A field guide to every generation. Explore each Pokémon, one record
            at a time.
          </p>
        </div>
        {data && !beyondLast ? (
          <div className="flex items-center gap-7 self-end md:self-center">
            <div className="text-right">
              <p className="text-display font-semibold tracking-tight text-foreground">
                {data.totalElements.toLocaleString()}
              </p>
              <p className="mt-1 font-mono text-label tracking-wide text-muted-foreground uppercase">
                Pokémon · all generations
              </p>
            </div>
            <img
              src={pokeballArchiveDiagram}
              alt=""
              width={134}
              height={134}
              className="size-[8.375rem] shrink-0"
            />
          </div>
        ) : null}
      </div>

      {isPending && !data ? <CatalogSkeleton /> : null}

      {isError && isUnavailable ? (
        <Alert className="rounded-md border-danger-border bg-danger-muted">
          <AlertTitle>Catalog unavailable</AlertTitle>
          <AlertDescription>
            The catalog service is temporarily unavailable. Try again in a
            moment.
          </AlertDescription>
          <Button type="button" className="mt-4" onClick={() => void refetch()}>
            Retry
          </Button>
        </Alert>
      ) : null}

      {isError && isNetworkError ? (
        <Alert className="rounded-md">
          <AlertTitle>Connection error</AlertTitle>
          <AlertDescription>
            We couldn’t connect to the server. Check your connection and try
            again.
          </AlertDescription>
          <Button type="button" className="mt-4" onClick={() => void refetch()}>
            Retry
          </Button>
        </Alert>
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
            This page is beyond the end of the catalog.
          </p>
          <Button asChild className="mt-6">
            <Link to="/?page=1">Back to page 1</Link>
          </Button>
        </div>
      ) : null}

      {data && !beyondLast && !isError ? (
        <div
          className={
            isFetching && isPlaceholderData ? 'opacity-80' : undefined
          }
        >
          <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
            <div className="flex flex-wrap items-baseline gap-x-3 gap-y-1">
              <p className="text-body font-semibold text-foreground">
                Pokémon records
              </p>
              <p className="font-mono text-caption tracking-wide text-muted-foreground">
                {formatRecordRange({
                  page,
                  size: pageSize,
                  totalElements: data.totalElements,
                  contentLength: data.content.length,
                })}
              </p>
            </div>
            <p className="font-mono text-label tracking-wide text-muted-foreground">
              H = Hidden ability
            </p>
          </div>
          <CatalogGrid items={data.content} />
          <CatalogPagination page={page} totalPages={data.totalPages} />
        </div>
      ) : null}
    </div>
  )
}
