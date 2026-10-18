import { describe, expect, it } from 'vitest'

import {
  buildSessionExpiredContext,
  sessionExpiredEntityId,
} from '@/features/auth/session-expired-context'

describe('buildSessionExpiredContext', () => {
  it('uses Pokémon details with entity name when available', () => {
    expect(buildSessionExpiredContext('/pokemon/25', '', 'Pikachu')).toBe(
      'Pokémon details / Pikachu',
    )
  })

  it('falls back to the route for unknown paths', () => {
    expect(buildSessionExpiredContext('/unknown', '?x=1')).toBe('/unknown?x=1')
  })
})

describe('sessionExpiredEntityId', () => {
  it('parses pokemon and collection item ids', () => {
    expect(sessionExpiredEntityId('/pokemon/25')).toBe(25)
    expect(sessionExpiredEntityId('/collection/1')).toBe(1)
    expect(sessionExpiredEntityId('/collection')).toBeNull()
  })
})
