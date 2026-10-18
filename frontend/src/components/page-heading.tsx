import type { ComponentProps } from 'react'

import { cn } from '@/lib/utils'

export function PageHeading({
  className,
  ...props
}: ComponentProps<'h1'>) {
  return (
    <h1
      tabIndex={-1}
      className={cn(
        'text-display font-semibold tracking-tight text-foreground outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50',
        className,
      )}
      {...props}
    />
  )
}
