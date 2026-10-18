import { type ClassValue, clsx } from 'clsx'
import { extendTailwindMerge } from 'tailwind-merge'

/**
 * Custom font-size tokens from @theme must live in the font-size group so
 * they do not collide with text-* color utilities (e.g. text-foreground).
 */
const twMerge = extendTailwindMerge({
  extend: {
    classGroups: {
      'font-size': [
        {
          text: [
            'label',
            'caption',
            'body-sm',
            'body',
            'title',
            'display',
            'hero',
          ],
        },
      ],
    },
  },
})

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}
