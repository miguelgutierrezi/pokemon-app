import { describe, expect, it } from 'vitest'

import { validateRegister } from '@/features/auth/validation'

const PASSWORD_MESSAGE =
  'Password must be at least 8 characters and at most 72 UTF-8 bytes.'

describe('validateRegister', () => {
  it('rejects a whitespace-only password with the standard password message', () => {
    const errors = validateRegister({
      name: 'Trainer',
      email: 't@pokefolio.dev',
      password: '        ',
    })
    expect(errors.password).toBe(PASSWORD_MESSAGE)
  })
})
