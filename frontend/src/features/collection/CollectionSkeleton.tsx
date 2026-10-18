import { Loader2 } from 'lucide-react'

import { Skeleton } from '@/components/ui/skeleton'

export function CollectionSkeleton() {
  return (
    <div
      data-testid="collection-skeleton"
      className="space-y-5"
      aria-busy="true"
    >
      <div className="flex items-center gap-3 text-muted-foreground">
        <Loader2 className="size-6 animate-spin text-primary" aria-hidden />
        <p className="text-body-sm">Loading the collection…</p>
      </div>
      <ul className="overflow-hidden rounded-xl border border-border bg-card">
        {Array.from({ length: 3 }, (_, index) => (
          <li
            key={index}
            className="flex items-center gap-5 border-t border-border p-5 first:border-t-0"
          >
            <Skeleton className="size-[72px] shrink-0 rounded-[10px]" />
            <div className="flex flex-1 flex-col gap-3">
              <Skeleton className="h-3.5 w-40" />
              <Skeleton className="h-2.5 w-24" />
            </div>
            <Skeleton className="hidden h-3 w-36 md:block" />
            <Skeleton className="hidden h-3 w-20 md:block" />
          </li>
        ))}
      </ul>
    </div>
  )
}
