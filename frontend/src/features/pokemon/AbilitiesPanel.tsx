import type { AbilitySummary } from '@/api/types'
import { formatSlug } from '@/lib/format'

export function AbilitiesPanel({ abilities }: { abilities: AbilitySummary[] }) {
  return (
    <section className="flex flex-col gap-6 rounded-lg border border-border bg-card p-5 md:p-7">
      <div className="flex items-center justify-between gap-3">
        <h2 className="text-title font-semibold text-foreground">Abilities</h2>
        <p className="font-mono text-label tracking-wide text-muted-foreground uppercase">
          Species traits
        </p>
      </div>
      <ul className="flex flex-col gap-4">
        {abilities.map((ability) => (
          <li
            key={`${ability.name}-${ability.hidden}`}
            className="flex items-center justify-between gap-3"
          >
            <p className="text-[0.9375rem] font-semibold text-foreground">
              {formatSlug(ability.name)}
            </p>
            <p className="font-mono text-[0.5625rem] tracking-wide text-muted-foreground uppercase">
              {ability.hidden ? 'Hidden ability' : 'Standard'}
            </p>
          </li>
        ))}
      </ul>
      <p className="text-caption text-muted-foreground">
        Hidden ability availability varies by game.
      </p>
    </section>
  )
}
