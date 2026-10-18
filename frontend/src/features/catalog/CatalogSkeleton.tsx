import { Skeleton } from '@/components/ui/skeleton'

export function CatalogSkeleton() {
  return (
    <div data-testid="catalog-skeleton" className="space-y-4" aria-busy="true">
      <p className="text-body-sm text-muted-foreground">Loading the catalog…</p>
      <ul className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {Array.from({ length: 9 }, (_, index) => (
          <li
            key={index}
            className="rounded-md border border-border bg-card p-5"
          >
            <div className="flex justify-between gap-3">
              <div className="space-y-2">
                <Skeleton className="h-3 w-12" />
                <Skeleton className="h-5 w-28" />
                <Skeleton className="h-3 w-24" />
              </div>
              <Skeleton className="size-24 rounded-md" />
            </div>
            <div className="mt-4 grid grid-cols-2 gap-3">
              <Skeleton className="h-8 w-full" />
              <Skeleton className="h-8 w-full" />
            </div>
            <Skeleton className="mt-4 h-8 w-2/3" />
          </li>
        ))}
      </ul>
    </div>
  )
}
