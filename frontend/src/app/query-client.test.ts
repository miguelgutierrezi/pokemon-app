import { describe, expect, it, vi } from 'vitest'

import { ApiError } from '@/api/api-error'
import {
  createAppQueryClient,
  listQueryOptions,
  queryRetry,
  STALE_TIME_MS,
} from '@/app/query-client'
import {
  getSessionExpiredOpen,
  resetSessionExpiredState,
} from '@/features/auth/session-expired'

describe('query client defaults', () => {
  it('uses a 5-minute staleTime', () => {
    expect(STALE_TIME_MS).toBe(5 * 60 * 1000)
  })

  it('documents list pagination placeholderData: keepPreviousData', () => {
    expect(listQueryOptions).toMatchObject({
      placeholderData: expect.any(Function),
    })
    expect(listQueryOptions.placeholderData.name).toMatch(/keepPreviousData/i)
  })

  it('never retries 4xx ApiError', () => {
    const error = new ApiError({
      status: 404,
      type: 'urn:problem-type:not-found',
      title: 'Not Found',
      detail: 'Missing',
    })
    expect(queryRetry(0, error)).toBe(false)
    expect(queryRetry(1, error)).toBe(false)
  })

  it('retries at most once for network (0) and 502', () => {
    const network = new ApiError({
      status: 0,
      type: 'network',
      title: 'Network Error',
      detail: 'Network error. Try again.',
    })
    const badGateway = new ApiError({
      status: 502,
      type: 'urn:problem-type:bad-gateway',
      title: 'Bad Gateway',
      detail: 'Upstream temporarily unavailable',
    })

    expect(queryRetry(0, network)).toBe(true)
    expect(queryRetry(1, network)).toBe(false)
    expect(queryRetry(0, badGateway)).toBe(true)
    expect(queryRetry(1, badGateway)).toBe(false)
  })

  it('never retries mutations', async () => {
    const signOut = vi.fn()
    const client = createAppQueryClient({ signOut })

    expect(client.getDefaultOptions().mutations?.retry).toBe(false)
  })

  it('on protected-write 401 clears session and opens session-expired signal', async () => {
    resetSessionExpiredState()
    const signOut = vi.fn()
    const client = createAppQueryClient({ signOut })

    const mutation = client
      .getMutationCache()
      .build(client, {
        mutationFn: async () => {
          throw new ApiError({
            status: 401,
            type: 'urn:problem-type:unauthorized',
            title: 'Unauthorized',
            detail: 'Missing or invalid access token',
          })
        },
        meta: { protectedWrite: true },
      })

    await expect(mutation.execute({})).rejects.toBeInstanceOf(ApiError)

    expect(signOut).toHaveBeenCalledTimes(1)
    expect(getSessionExpiredOpen()).toBe(true)
  })

  it('does not open session-expired on non-protected 401 mutations', async () => {
    resetSessionExpiredState()
    const signOut = vi.fn()
    const client = createAppQueryClient({ signOut })

    const mutation = client.getMutationCache().build(client, {
      mutationFn: async () => {
        throw new ApiError({
          status: 401,
          type: 'urn:problem-type:unauthorized',
          title: 'Unauthorized',
          detail: 'Invalid email or password',
        })
      },
    })

    await expect(mutation.execute({})).rejects.toBeInstanceOf(ApiError)
    expect(signOut).not.toHaveBeenCalled()
    expect(getSessionExpiredOpen()).toBe(false)
  })
})
