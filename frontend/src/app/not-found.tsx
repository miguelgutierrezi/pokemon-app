import { Link } from 'react-router'

import { Button } from '@/components/ui/button'
import { PageHeading } from '@/components/page-heading'

export function NotFoundPage() {
  return (
    <div className="flex min-h-[70vh] flex-col items-center justify-center px-6 py-16">
      <div className="flex w-full max-w-md flex-col items-center text-center">
        <div className="flex items-center gap-2">
          <span className="h-0.5 w-4 bg-primary" aria-hidden />
          <p className="font-mono text-label tracking-wide text-muted-foreground uppercase">
            Pokéfolio / Navigation
          </p>
        </div>
        <p
          className="mt-4 font-mono text-hero leading-none font-medium text-foreground"
          aria-hidden
        >
          404
        </p>
        <PageHeading className="mt-2 leading-tight">Page not found</PageHeading>
        <p className="mt-3 text-body leading-relaxed text-muted-foreground">
          The link may be incorrect, or this page may no longer exist.
        </p>
        <Button asChild className="mt-7 h-14 rounded-md px-6 text-body">
          <Link to="/">Back to catalog</Link>
        </Button>
      </div>
    </div>
  )
}
