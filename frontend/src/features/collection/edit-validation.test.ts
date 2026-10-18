import { describe, expect, it } from 'vitest'

import {
  appendTagsFromCommaInput,
  commitPendingTag,
} from '@/features/collection/edit-validation'

describe('commitPendingTag', () => {
  it('adds a normalised tag and clears the draft', () => {
    expect(commitPendingTag([], 'Starter')).toEqual({
      tags: ['starter'],
      draft: '',
    })
  })

  it('rejects overlong tags with an error', () => {
    const draft = 'x'.repeat(33)
    const result = commitPendingTag([], draft)
    expect(result.error).toMatch(/32/)
    expect(result.tags).toEqual([])
    expect(result.draft).toBe(draft)
  })
})

describe('appendTagsFromCommaInput', () => {
  it('adds complete parts and keeps the trailing fragment', () => {
    expect(appendTagsFromCommaInput([], 'fire, water')).toEqual({
      tags: ['fire'],
      draft: ' water',
    })
  })

  it('adds every complete part when the value ends with a comma', () => {
    expect(appendTagsFromCommaInput([], 'fire, water,')).toEqual({
      tags: ['fire', 'water'],
      draft: '',
    })
  })
})
