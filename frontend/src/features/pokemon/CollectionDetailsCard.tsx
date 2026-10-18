import { FileText } from 'lucide-react'

import type { LocalPokemon } from '@/api/types'
import { Badge } from '@/components/ui/badge'
import { formatOptionalText } from '@/lib/format'

export function CollectionDetailsCard({ local }: { local: LocalPokemon }) {
  return (
    <section className="rounded-lg border border-border bg-card p-5 md:p-7">
      <div className="flex items-center gap-2.5">
        <FileText className="size-5 text-primary" aria-hidden />
        <h2 className="text-title font-semibold text-foreground">
          Collection details
        </h2>
      </div>
      <p className="mt-2 text-caption text-muted-foreground">
        Illustrative personal data · separate from the official Pokédex record.
      </p>
      <dl className="mt-6 space-y-5">
        <div className="space-y-2">
          <dt className="font-mono text-label tracking-wide text-muted-foreground uppercase">
            Localized name
          </dt>
          <dd className="text-body font-medium text-foreground">
            {formatOptionalText(local.localizedName)}
          </dd>
        </div>
        <div className="space-y-2">
          <dt className="font-mono text-label tracking-wide text-muted-foreground uppercase">
            Location
          </dt>
          <dd className="text-body font-medium text-foreground">
            {formatOptionalText(local.location)}
          </dd>
        </div>
        <div className="space-y-2">
          <dt className="font-mono text-label tracking-wide text-muted-foreground uppercase">
            Tags
          </dt>
          <dd>
            {local.tags.length === 0 ? (
              <span className="text-body font-medium text-foreground">—</span>
            ) : (
              <ul className="flex flex-wrap gap-2">
                {local.tags.map((tag) => (
                  <li key={tag}>
                    <Badge
                      variant="outline"
                      className="rounded-sm px-2.5 py-1 text-caption font-normal"
                    >
                      {tag}
                    </Badge>
                  </li>
                ))}
              </ul>
            )}
          </dd>
        </div>
      </dl>
      <p className="mt-6 text-caption text-muted-foreground">
        Stored in the shared collection on the server.
      </p>
    </section>
  )
}
