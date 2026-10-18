import { Badge } from '@/components/ui/badge'
import { formatSlug } from '@/lib/format'
import { cn } from '@/lib/utils'

const TYPE_CLASS: Record<string, string> = {
  grass: 'bg-type-grass text-type-grass-foreground',
  poison: 'bg-type-poison text-type-poison-foreground',
  normal: 'bg-type-normal text-type-normal-foreground',
}

export function TypeBadge({ type }: { type: string }) {
  const key = type.toLowerCase()
  return (
    <Badge
      variant="secondary"
      className={cn(
        'rounded-sm border-transparent px-3 py-1.5 text-caption font-semibold',
        TYPE_CLASS[key] ?? 'bg-muted text-muted-foreground',
      )}
    >
      {formatSlug(type)}
    </Badge>
  )
}
