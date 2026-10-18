import { describe, expect, it } from 'vitest'

import { cn } from '@/lib/utils'

describe('cn / extendTailwindMerge font-size tokens', () => {
  it('keeps custom font-size and text color together', () => {
    expect(cn('text-display', 'text-foreground')).toBe(
      'text-display text-foreground',
    )
  })

  it('resolves conflicting font-size utilities to the last one', () => {
    expect(cn('text-xs', 'text-caption')).toBe('text-caption')
  })
})
