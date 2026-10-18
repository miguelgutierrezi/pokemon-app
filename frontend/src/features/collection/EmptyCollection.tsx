import { BookmarkPlus } from 'lucide-react'
import { Link } from 'react-router'

import { Button } from '@/components/ui/button'

export function EmptyCollection() {
  return (
    <div className="flex flex-col items-center gap-5 rounded-xl border border-border bg-card p-10 text-center">
      <div className="flex size-14 items-center justify-center rounded-xl bg-surface">
        <BookmarkPlus className="size-7 text-muted-foreground" aria-hidden />
      </div>
      <p className="text-display font-semibold text-foreground">
        The collection is empty
      </p>
      <p className="max-w-md text-[0.8125rem] leading-[1.6] text-muted-foreground">
        Start with a Pokémon you love. Browse the catalog and save your first
        Pokémon to the shared collection.
      </p>
      <Button asChild className="h-12 min-w-[240px] rounded-[10px]">
        <Link to="/">Browse the catalog</Link>
      </Button>
    </div>
  )
}
