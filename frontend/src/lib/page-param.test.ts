import { describe, expect, it } from 'vitest'

import { ApiError } from '@/api/api-error'
import { isBeyondLastPageError, parsePageParam } from '@/lib/page-param'

describe('parsePageParam', () => {
  it('defaults missing or empty to 1', () => {
    expect(parsePageParam(null)).toBe(1)
    expect(parsePageParam('')).toBe(1)
    expect(parsePageParam('   ')).toBe(1)
  })

  it('accepts positive integers', () => {
    expect(parsePageParam('1')).toBe(1)
    expect(parsePageParam('12')).toBe(12)
  })

  it('rejects zero, negative, and non-numeric', () => {
    expect(parsePageParam('0')).toBeNull()
    expect(parsePageParam('-1')).toBeNull()
    expect(parsePageParam('abc')).toBeNull()
    expect(parsePageParam('1.5')).toBeNull()
  })
})

describe('isBeyondLastPageError', () => {
  it('is true only for 400 when page > 1', () => {
    const error = new ApiError({
      status: 400,
      type: 'urn:problem-type:bad-request',
      title: 'Bad Request',
      detail: 'beyond last page',
    })
    expect(isBeyondLastPageError(error, 2)).toBe(true)
    expect(isBeyondLastPageError(error, 1)).toBe(false)
    expect(isBeyondLastPageError(new Error('x'), 2)).toBe(false)
  })
})
