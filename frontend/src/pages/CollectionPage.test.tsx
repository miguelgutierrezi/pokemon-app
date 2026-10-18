import { act, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { ApiError } from '@/api/api-error'
import type { LocalPokemon, LocalPokemonPage } from '@/api/types'
import { AppProviders } from '@/app/providers'
import { routerConfig } from '@/app/routes'
import { SESSION_KEY } from '@/lib/session'

vi.mock('@/api/collection', () => ({
  listCollection: vi.fn(),
  getLocalPokemonById: vi.fn(),
  updateLocalPokemon: vi.fn(),
  deleteLocalPokemon: vi.fn(),
  importPokemon: vi.fn(),
}))

import { getLocalPokemonById, listCollection } from '@/api/collection'

const listCollectionMock = vi.mocked(listCollection)
const getLocalPokemonByIdMock = vi.mocked(getLocalPokemonById)

function localPokemon(overrides: Partial<LocalPokemon> = {}): LocalPokemon {
  return {
    nationalNumber: 25,
    name: 'Pikachu',
    types: ['electric'],
    heightM: 0.4,
    weightKg: 6.0,
    category: 'Mouse Pokémon',
    generation: 'generation-i',
    imageUrl: 'https://example.com/artwork/25.png',
    syncedAt: '2026-01-01T00:00:00Z',
    localizedName: 'Sparky',
    location: 'Viridian Forest',
    tags: ['starter', 'favorite'],
    version: 3,
    ...overrides,
  }
}

function pageResult(
  content: LocalPokemon[],
  overrides: Partial<LocalPokemonPage> = {},
): LocalPokemonPage {
  return {
    content,
    page: 1,
    size: 9,
    totalElements: content.length,
    totalPages: 1,
    ...overrides,
  }
}

function nineEntries(): LocalPokemon[] {
  return Array.from({ length: 9 }, (_, index) =>
    localPokemon({
      nationalNumber: index + 1,
      name: `Pokemon${index + 1}`,
      imageUrl: `https://example.com/artwork/${index + 1}.png`,
      localizedName: index === 0 ? 'Alpha' : null,
      location: index === 0 ? 'Pallet Town' : null,
      tags: index === 0 ? ['field'] : [],
    }),
  )
}

function seedSession() {
  localStorage.setItem(
    SESSION_KEY,
    JSON.stringify({
      token: 'auth-token',
      expiresAt: Date.now() + 60_000,
      user: { id: 1, name: 'Demo', email: 'demo@pokefolio.dev' },
    }),
  )
}

function renderCollection(initialEntry = '/collection') {
  const router = createMemoryRouter(routerConfig, {
    initialEntries: [initialEntry],
  })
  render(
    <AppProviders>
      <RouterProvider router={router} />
    </AppProviders>,
  )
  return router
}

describe('CollectionPage', () => {
  beforeEach(() => {
    listCollectionMock.mockReset()
    getLocalPokemonByIdMock.mockReset()
    localStorage.clear()
    getLocalPokemonByIdMock.mockImplementation(async (id) =>
      localPokemon({ nationalNumber: id }),
    )
  })

  it('loads /collection and ?page=N with size 9 and no Authorization', async () => {
    listCollectionMock.mockResolvedValue(
      pageResult(nineEntries(), { totalPages: 2, totalElements: 18 }),
    )

    renderCollection('/collection')
    await waitFor(() => expect(listCollectionMock).toHaveBeenCalled())
    expect(listCollectionMock).toHaveBeenCalledWith(
      { page: 1, size: 9 },
      expect.any(AbortSignal),
    )

    listCollectionMock.mockClear()
    renderCollection('/collection?page=2')
    await waitFor(() => expect(listCollectionMock).toHaveBeenCalled())
    expect(listCollectionMock).toHaveBeenCalledWith(
      { page: 2, size: 9 },
      expect.any(AbortSignal),
    )
  })

  it('renders entries with image, metadata, proprietary fields, and detail link', async () => {
    listCollectionMock.mockResolvedValue(
      pageResult([
        localPokemon({
          nationalNumber: 25,
          name: 'Pikachu',
          category: 'Mouse Pokémon',
          types: ['electric'],
          localizedName: 'Sparky',
          location: 'Viridian Forest',
          tags: ['starter', 'favorite'],
          version: 7,
        }),
        localPokemon({
          nationalNumber: 1,
          name: 'Bulbasaur',
          category: 'Seed Pokémon',
          types: ['grass', 'poison'],
          localizedName: null,
          location: null,
          tags: [],
        }),
      ], { totalElements: 2 }),
    )

    renderCollection('/collection')

    await waitFor(() => {
      expect(screen.getByRole('link', { name: /pikachu/i })).toBeInTheDocument()
    })

    const card = screen.getByRole('article', { name: /pikachu/i })
    const img = within(card).getByRole('img', { name: 'Pikachu' })
    expect(img).toHaveAttribute('src', 'https://example.com/artwork/25.png')
    expect(img).toHaveAttribute('width')
    expect(img).toHaveAttribute('height')
    expect(img).toHaveAttribute('loading', 'lazy')

    expect(within(card).getByText('#0025')).toBeInTheDocument()
    expect(within(card).getByText('Mouse Pokémon')).toBeInTheDocument()
    expect(within(card).getByText('Electric')).toBeInTheDocument()
    expect(within(card).getByText('Sparky')).toBeInTheDocument()
    expect(within(card).getByText('Viridian Forest')).toBeInTheDocument()
    expect(within(card).getByText('starter')).toBeInTheDocument()
    expect(within(card).getByText('favorite')).toBeInTheDocument()
    expect(within(card).queryByText('7')).not.toBeInTheDocument()
    expect(
      within(card).getByRole('link', { name: /view details/i }),
    ).toHaveAttribute('href', '/pokemon/25')
    expect(within(card).getByRole('link', { name: /^Edit$/i })).toBeInTheDocument()
    expect(
      within(card).getByRole('button', { name: /^Remove$/i }),
    ).toBeInTheDocument()

    const emptyProps = screen.getByRole('article', { name: /bulbasaur/i })
    expect(within(emptyProps).getAllByText('—').length).toBeGreaterThanOrEqual(2)
    expect(
      within(emptyProps).getByRole('link', { name: /view details/i }),
    ).toBeInTheDocument()
    expect(
      within(emptyProps).getByRole('link', { name: /^Edit$/i }),
    ).toBeInTheDocument()
    expect(
      within(emptyProps).getByRole('button', { name: /^Remove$/i }),
    ).toBeInTheDocument()

    expect(screen.getAllByText(/shared collection/i).length).toBeGreaterThan(0)
    expect(screen.queryByText(/your account/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/my collection/i)).not.toBeInTheDocument()
    expect(
      screen.getByText(/Saved Pokémon · all generations/i),
    ).toBeInTheDocument()
    expect(screen.getByText('2')).toBeInTheDocument()
  })

  it('shows loading skeleton before data arrives', async () => {
    let resolvePage!: (value: LocalPokemonPage) => void
    listCollectionMock.mockImplementation(
      () =>
        new Promise<LocalPokemonPage>((resolve) => {
          resolvePage = resolve
        }),
    )

    renderCollection('/collection')

    expect(screen.getByTestId('collection-skeleton')).toBeInTheDocument()
    expect(screen.queryByRole('article', { name: /pikachu/i })).not.toBeInTheDocument()

    await act(async () => {
      resolvePage(pageResult([localPokemon()]))
    })

    await waitFor(() => {
      expect(screen.getByRole('article', { name: /pikachu/i })).toBeInTheDocument()
    })
  })

  it('keeps previous list visible while loading page 2 (keepPreviousData)', async () => {
    const user = userEvent.setup()
    listCollectionMock.mockResolvedValueOnce(
      pageResult(nineEntries(), { page: 1, totalPages: 2, totalElements: 18 }),
    )

    let resolvePage2!: (value: LocalPokemonPage) => void
    listCollectionMock.mockImplementationOnce(
      () =>
        new Promise<LocalPokemonPage>((resolve) => {
          resolvePage2 = resolve
        }),
    )

    const router = renderCollection('/collection')
    await waitFor(() => {
      expect(screen.getByRole('article', { name: /pokemon1/i })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('link', { name: /^2$/ }))

    await waitFor(() => {
      expect(router.state.location.search).toBe('?page=2')
    })
    expect(screen.getByRole('article', { name: /pokemon1/i })).toBeInTheDocument()

    await act(async () => {
      resolvePage2(
        pageResult(
          [
            localPokemon({
              nationalNumber: 10,
              name: 'Page2Mon',
              imageUrl: 'https://example.com/artwork/10.png',
            }),
          ],
          { page: 2, totalPages: 2, totalElements: 18 },
        ),
      )
    })

    await waitFor(() => {
      expect(screen.getByRole('article', { name: /page2mon/i })).toBeInTheDocument()
    })
  })

  it('replaces invalid page with page=1 without calling API for the invalid value', async () => {
    listCollectionMock.mockResolvedValue(pageResult([localPokemon()]))

    for (const entry of [
      '/collection?page=0',
      '/collection?page=-1',
      '/collection?page=abc',
    ]) {
      listCollectionMock.mockClear()
      const router = renderCollection(entry)

      await waitFor(() => {
        expect(router.state.location.search).toBe('?page=1')
      })
      await waitFor(() => expect(listCollectionMock).toHaveBeenCalled())
      expect(
        listCollectionMock.mock.calls.every(([query]) => query.page === 1),
      ).toBe(true)
    }
  })

  it('shows beyond-last empty state with link to page 1 on API 400', async () => {
    listCollectionMock.mockRejectedValue(
      new ApiError({
        status: 400,
        type: 'urn:problem-type:bad-request',
        title: 'Bad Request',
        detail: 'page 5 is beyond last page 1',
      }),
    )

    renderCollection('/collection?page=5')

    await waitFor(() => {
      expect(screen.getByText('No records on this page')).toBeInTheDocument()
    })
    expect(screen.queryByText(/something went wrong/i)).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Back to page 1' })).toHaveAttribute(
      'href',
      '/collection?page=1',
    )
  })

  it('shows empty collection with Browse the catalog CTA', async () => {
    listCollectionMock.mockResolvedValue(
      pageResult([], { totalElements: 0, totalPages: 0 }),
    )

    renderCollection('/collection')

    await waitFor(() => {
      expect(screen.getByText(/collection is empty/i)).toBeInTheDocument()
    })
    expect(
      screen.getByRole('link', { name: /Browse the catalog/i }),
    ).toHaveAttribute('href', '/')
  })

  it('shows collection unavailable on 502 with Retry', async () => {
    const user = userEvent.setup()
    const unavailable = new ApiError({
      status: 502,
      type: 'urn:problem-type:bad-gateway',
      title: 'Bad Gateway',
      detail: 'Upstream temporarily unavailable',
    })
    listCollectionMock
      .mockRejectedValueOnce(unavailable)
      .mockRejectedValueOnce(unavailable)
      .mockResolvedValueOnce(pageResult([localPokemon()]))

    renderCollection('/collection')

    await waitFor(() => {
      expect(screen.getByText(/collection unavailable/i)).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: /retry|try again/i }))

    await waitFor(() => {
      expect(screen.getByRole('article', { name: /pikachu/i })).toBeInTheDocument()
    })
  })

  it('shows connection-error state on network failure with Retry', async () => {
    const user = userEvent.setup()
    const network = new ApiError({
      status: 0,
      type: 'network',
      title: 'Network Error',
      detail: 'Network error. Try again.',
    })
    listCollectionMock
      .mockRejectedValueOnce(network)
      .mockRejectedValueOnce(network)
      .mockResolvedValueOnce(pageResult([localPokemon()]))

    renderCollection('/collection')

    await waitFor(() => {
      expect(screen.getByText(/connection error|couldn’t load|couldn't load/i)).toBeInTheDocument()
    })
    expect(screen.queryByText(/collection unavailable/i)).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /retry|try again/i }))

    await waitFor(() => {
      expect(screen.getByRole('article', { name: /pikachu/i })).toBeInTheDocument()
    })
  })

  it('signed out: Edit and Remove go to login with redirect to current collection page', async () => {
    const user = userEvent.setup()
    listCollectionMock.mockResolvedValue(pageResult([localPokemon()], { totalElements: 1 }))

    const router = renderCollection('/collection?page=2')
    await waitFor(() => {
      expect(screen.getByRole('article', { name: /pikachu/i })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('link', { name: /^Edit$/i }))
    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/login')
    })
    expect(router.state.location.search).toBe(
      `?redirect=${encodeURIComponent('/collection?page=2')}`,
    )
  })

  it('signed out: Remove goes to login with redirect', async () => {
    const user = userEvent.setup()
    listCollectionMock.mockResolvedValue(pageResult([localPokemon()], { totalElements: 1 }))

    const router = renderCollection('/collection')
    await waitFor(() => {
      expect(screen.getByRole('article', { name: /pikachu/i })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: /^Remove$/i }))
    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/login')
    })
    expect(router.state.location.search).toBe(
      `?redirect=${encodeURIComponent('/collection')}`,
    )
  })

  it('signed in: Edit navigates to /collection/:id', async () => {
    seedSession()
    const user = userEvent.setup()
    listCollectionMock.mockResolvedValue(pageResult([localPokemon()], { totalElements: 1 }))

    const router = renderCollection('/collection')
    await waitFor(() => {
      expect(screen.getByRole('article', { name: /pikachu/i })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('link', { name: /^Edit$/i }))
    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/collection/25')
    })
  })
})
