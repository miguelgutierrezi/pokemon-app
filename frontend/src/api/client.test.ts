import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { ApiError } from '@/api/api-error'
import { createApiClient } from '@/api/client'
import {
  deleteLocalPokemon,
  getCollection,
  getLocalPokemon,
  importPokemon,
  updateLocalPokemon,
} from '@/api/collection'
import { login, register } from '@/api/auth'
import { getPokemon, getPokemonPage } from '@/api/pokemon'

function jsonResponse(
  body: unknown,
  init: { status?: number; contentType?: string } = {},
): Response {
  const status = init.status ?? 200
  const contentType = init.contentType ?? 'application/json'
  return new Response(body === undefined ? null : JSON.stringify(body), {
    status,
    headers: { 'Content-Type': contentType },
  })
}

function problemResponse(
  status: number,
  problem: {
    type: string
    title: string
    detail: string
  },
): Response {
  return new Response(JSON.stringify({ ...problem, status }), {
    status,
    headers: { 'Content-Type': 'application/problem+json' },
  })
}

describe('api client', () => {
  const fetchMock = vi.fn()

  beforeEach(() => {
    fetchMock.mockReset()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('never sends Authorization on catalog/collection GET even when token exists', async () => {
    fetchMock.mockImplementation(() =>
      Promise.resolve(
        jsonResponse({
          content: [],
          page: 1,
          size: 9,
          totalElements: 0,
          totalPages: 0,
        }),
      ),
    )
    const client = createApiClient({ getToken: () => 'secret-token' })

    await getPokemonPage(client, { page: 2, size: 9 })
    await getCollection(client, { page: 1, size: 9 })
    await getPokemon(client, 1)
    await getLocalPokemon(client, 1)

    expect(fetchMock).toHaveBeenCalledTimes(4)
    for (const [url, init] of fetchMock.mock.calls) {
      expect(String(url)).toMatch(/^\/api\/v1\//)
      const headers = new Headers(init?.headers)
      expect(headers.has('Authorization')).toBe(false)
    }

    const [catalogUrl] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(catalogUrl).toBe('/api/v1/pokemon?page=2&size=9')
  })

  it('never sends Authorization on auth login/register', async () => {
    fetchMock.mockImplementation(() =>
      Promise.resolve(
        jsonResponse({
          accessToken: 't',
          tokenType: 'Bearer',
          expiresIn: 3600,
          user: { id: 1, name: 'Ash', email: 'ash@example.com' },
        }),
      ),
    )
    const client = createApiClient({ getToken: () => 'secret-token' })

    await login(client, { email: 'a@b.c', password: 'x' })
    await register(client, {
      name: 'Ash',
      email: 'a@b.c',
      password: 'password1',
    })

    for (const [, init] of fetchMock.mock.calls) {
      const headers = new Headers(init?.headers)
      expect(headers.has('Authorization')).toBe(false)
    }
  })

  it('sends Bearer on POST/PUT/DELETE collection when token is available', async () => {
    fetchMock
      .mockResolvedValueOnce(
        jsonResponse(
          {
            nationalNumber: 25,
            name: 'Pikachu',
            types: ['Electric'],
            heightM: 0.4,
            weightKg: 6,
            category: 'Mouse Pokémon',
            generation: 'Generation I',
            imageUrl: 'https://example.com/25.png',
            syncedAt: '2026-01-01T00:00:00Z',
            localizedName: null,
            location: null,
            tags: [],
            version: 0,
          },
          { status: 201 },
        ),
      )
      .mockResolvedValueOnce(
        jsonResponse({
          nationalNumber: 25,
          name: 'Pikachu',
          types: ['Electric'],
          heightM: 0.4,
          weightKg: 6,
          category: 'Mouse Pokémon',
          generation: 'Generation I',
          imageUrl: 'https://example.com/25.png',
          syncedAt: '2026-01-01T00:00:00Z',
          localizedName: 'Sparky',
          location: null,
          tags: [],
          version: 1,
        }),
      )
      .mockResolvedValueOnce(new Response(null, { status: 204 }))

    const client = createApiClient({ getToken: () => 'secret-token' })

    await importPokemon(client, { nationalNumber: 25 })
    await updateLocalPokemon(client, 25, {
      localizedName: 'Sparky',
      location: null,
      tags: [],
      version: 0,
    })
    await deleteLocalPokemon(client, 25)

    expect(fetchMock.mock.calls.map(([url]) => String(url))).toEqual([
      '/api/v1/collection',
      '/api/v1/collection/25',
      '/api/v1/collection/25',
    ])
    expect(
      (fetchMock.mock.calls[0] as [string, RequestInit])[1].method,
    ).toBe('POST')
    expect(
      (fetchMock.mock.calls[1] as [string, RequestInit])[1].method,
    ).toBe('PUT')
    expect(
      (fetchMock.mock.calls[2] as [string, RequestInit])[1].method,
    ).toBe('DELETE')

    for (const [, init] of fetchMock.mock.calls) {
      const headers = new Headers(init?.headers)
      expect(headers.get('Authorization')).toBe('Bearer secret-token')
    }
  })

  it('parses 2xx JSON and returns undefined for 204', async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ name: 'Bulbasaur' }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))

    const client = createApiClient({ getToken: () => null })

    await expect(
      client.request<{ name: string }>('/api/v1/pokemon/1'),
    ).resolves.toEqual({ name: 'Bulbasaur' })
    await expect(
      client.request<undefined>('/api/v1/collection/1', {
        method: 'DELETE',
        auth: true,
      }),
    ).resolves.toBeUndefined()
  })

  it('maps problem+json statuses including field extraction', async () => {
    const client = createApiClient({ getToken: () => 't' })

    fetchMock.mockResolvedValueOnce(
      problemResponse(400, {
        type: 'urn:problem-type:bad-request',
        title: 'Bad Request',
        detail: "Invalid value for parameter 'password'",
      }),
    )
    await expect(
      register(client, { name: 'A', email: 'a@b.c', password: 'short' }),
    ).rejects.toMatchObject({
      status: 400,
      field: 'password',
    } satisfies Partial<ApiError>)

    fetchMock.mockResolvedValueOnce(
      problemResponse(400, {
        type: 'urn:problem-type:bad-request',
        title: 'Bad Request',
        detail: "Unknown or read-only property 'name'",
      }),
    )
    await expect(
      updateLocalPokemon(client, 1, {
        localizedName: null,
        location: null,
        tags: [],
        version: 0,
      }),
    ).rejects.toMatchObject({ field: 'name' })

    fetchMock.mockResolvedValueOnce(
      problemResponse(400, {
        type: 'urn:problem-type:bad-request',
        title: 'Bad Request',
        detail: "Invalid or missing value for property 'version'",
      }),
    )
    await expect(
      updateLocalPokemon(client, 1, {
        localizedName: null,
        location: null,
        tags: [],
        version: 0,
      }),
    ).rejects.toMatchObject({ field: 'version' })

    fetchMock.mockResolvedValueOnce(
      problemResponse(401, {
        type: 'urn:problem-type:unauthorized',
        title: 'Unauthorized',
        detail: 'Missing or invalid access token',
      }),
    )
    await expect(
      importPokemon(client, { nationalNumber: 1 }),
    ).rejects.toMatchObject({
      status: 401,
      type: 'urn:problem-type:unauthorized',
    })

    fetchMock.mockResolvedValueOnce(
      problemResponse(404, {
        type: 'urn:problem-type:not-found',
        title: 'Not Found',
        detail: 'Not found',
      }),
    )
    await expect(getPokemon(client, 999999)).rejects.toMatchObject({
      status: 404,
      type: 'urn:problem-type:not-found',
    })

    fetchMock.mockResolvedValueOnce(
      problemResponse(409, {
        type: 'urn:problem-type:conflict',
        title: 'Conflict',
        detail: 'Already in local collection',
      }),
    )
    await expect(
      importPokemon(client, { nationalNumber: 1 }),
    ).rejects.toMatchObject({ type: 'urn:problem-type:conflict' })

    fetchMock.mockResolvedValueOnce(
      problemResponse(409, {
        type: 'urn:problem-type:stale-version',
        title: 'Conflict',
        detail: 'Modified elsewhere',
      }),
    )
    await expect(
      updateLocalPokemon(client, 1, {
        localizedName: null,
        location: null,
        tags: [],
        version: 0,
      }),
    ).rejects.toMatchObject({ type: 'urn:problem-type:stale-version' })

    fetchMock.mockResolvedValueOnce(
      problemResponse(502, {
        type: 'urn:problem-type:bad-gateway',
        title: 'Bad Gateway',
        detail: 'Upstream temporarily unavailable',
      }),
    )
    await expect(getPokemonPage(client, { page: 1, size: 9 })).rejects.toMatchObject({
      status: 502,
      type: 'urn:problem-type:bad-gateway',
    })
  })

  it('maps non-problem error bodies to ApiError with status and generic detail', async () => {
    fetchMock.mockImplementation(() =>
      Promise.resolve(
        new Response('<html>oops</html>', {
          status: 500,
          headers: { 'Content-Type': 'text/html' },
        }),
      ),
    )
    const client = createApiClient({ getToken: () => null })

    await expect(getPokemon(client, 1)).rejects.toMatchObject({
      status: 500,
      type: '',
      detail: expect.stringMatching(/try again|unexpected|error/i),
    })
  })

  it('keeps generic detail for HTML 502 bodies', async () => {
    fetchMock.mockImplementation(() =>
      Promise.resolve(
        new Response('<html>Bad Gateway</html>', {
          status: 502,
          headers: { 'Content-Type': 'text/html' },
        }),
      ),
    )
    const client = createApiClient({ getToken: () => null })

    const error = await getPokemon(client, 1).catch((e: unknown) => e)
    expect(error).toMatchObject({ status: 502 })
    expect((error as ApiError).detail).not.toMatch(/<html/i)
  })

  it('maps network failure to ApiError status 0 type network', async () => {
    fetchMock.mockRejectedValue(new TypeError('Failed to fetch'))
    const client = createApiClient({ getToken: () => null })

    await expect(getPokemon(client, 1)).rejects.toMatchObject({
      status: 0,
      type: 'network',
      detail: expect.stringMatching(/network/i),
    })
  })

  it('rethrows AbortError unchanged', async () => {
    const abortError = new DOMException('The operation was aborted.', 'AbortError')
    fetchMock.mockRejectedValue(abortError)
    const client = createApiClient({ getToken: () => null })
    const controller = new AbortController()
    controller.abort()

    await expect(
      client.request('/api/v1/pokemon/1', { signal: controller.signal }),
    ).rejects.toBe(abortError)
    await expect(
      client.request('/api/v1/pokemon/1', { signal: controller.signal }),
    ).rejects.not.toBeInstanceOf(ApiError)
  })

  it('passes AbortSignal through to fetch', async () => {
    fetchMock.mockImplementation(() =>
      Promise.resolve(
        jsonResponse({
          content: [],
          page: 1,
          size: 9,
          totalElements: 0,
          totalPages: 0,
        }),
      ),
    )
    const client = createApiClient({ getToken: () => null })
    const controller = new AbortController()

    await getPokemonPage(client, { page: 1, size: 9 }, controller.signal)

    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(init.signal).toBe(controller.signal)
  })
})
