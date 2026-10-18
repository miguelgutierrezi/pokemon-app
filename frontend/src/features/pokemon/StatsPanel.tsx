import type { PokemonStats } from '@/api/types'
import { cn } from '@/lib/utils'

const STAT_MAX = 255

const STAT_ROWS: { key: keyof Omit<PokemonStats, 'total'>; label: string }[] = [
  { key: 'hp', label: 'HP' },
  { key: 'attack', label: 'Attack' },
  { key: 'defense', label: 'Defense' },
  { key: 'specialAttack', label: 'Special Attack' },
  { key: 'specialDefense', label: 'Special Defense' },
  { key: 'speed', label: 'Speed' },
]

export function StatsPanel({ stats }: { stats: PokemonStats }) {
  return (
    <section className="flex flex-col gap-5 rounded-lg border border-border bg-card p-5 md:p-7">
      <div className="flex items-center justify-between gap-3">
        <h2 className="text-title font-semibold text-foreground">Base stats</h2>
        <p className="font-mono text-caption tracking-wide text-muted-foreground uppercase">
          TOTAL {stats.total}
        </p>
      </div>
      <ul className="flex flex-col gap-4">
        {STAT_ROWS.map(({ key, label }) => {
          const value = stats[key]
          const accent = key === 'specialAttack' || key === 'specialDefense'
          return (
            <li key={key} className="flex items-center gap-3">
              <span className="w-[7.25rem] shrink-0 text-caption text-foreground md:w-[8.25rem]">
                {label}
              </span>
              <span className="w-6 shrink-0 text-right font-mono text-body-sm text-foreground">
                {value}
              </span>
              <div className="h-1.5 min-w-0 flex-1 overflow-hidden rounded-sm bg-border">
                <div
                  className={cn(
                    'h-full rounded-sm',
                    accent ? 'bg-primary' : 'bg-foreground',
                  )}
                  style={{ width: `${Math.min(100, (value / STAT_MAX) * 100)}%` }}
                />
              </div>
            </li>
          )
        })}
      </ul>
      <p className="text-caption text-muted-foreground">
        Species base values · scale 0–255
      </p>
    </section>
  )
}
