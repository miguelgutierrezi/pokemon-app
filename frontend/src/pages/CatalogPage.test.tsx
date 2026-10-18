import { act, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { ApiError } from '@/api/api-error'
import type { PokemonPage, PokemonSummary } from '@/api/types'
import { AppProviders } from '@/app/providers'
import { routerConfig } from '@/app/routes'

vi.mock('@/api/pokemon', () => ({
  listPokemon: vi.fn(),
}))

import { listPokemon } from '@/api/pokemon'

const listPokemonMock = vi.mocked(listPokemon)

function pokemon(overrides: Partial<PokemonSummary> = {}): PokemonSummary {
  return {
    nationalNumber: 1,
    name: 'Bulbasaur',
    spriteUrl: 'https://example.com/1.png',
    category: 'Seed Pokémon',
    weightKg: 6.9,
    abilities: [
      { name: 'overgrow', hidden: false },
      { name: 'chlorophyll', hidden: true },
    ],
    ...overrides,
  }
}

function pageResult(
  content: PokemonSummary[],
  overrides: Partial<PokemonPage> = {},
): PokemonPage {
  return {
    content,
    page: 1,
    size: 9,
    totalElements: content.length,
    totalPages: 1,
    ...overrides,
  }
}

function ninePokemon(): PokemonSummary[] {
  return Array.from({ length: 9 }, (_, index) =>
    pokemon({
      nationalNumber: index + 1,
      name: `Pokemon${index + 1}`,
      spriteUrl: `https://example.com/${index + 1}.png`,
      weightKg: index === 0 ? 6.9 : 10 + index,
    }),
  )
}

function renderCatalog(initialEntry = '/') {
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

describe('CatalogPage', () => {
  beforeEach(() => {
    listPokemonMock.mockReset()
    localStorage.clear()
  })

  it('loads page 1 for / and ?page=1 with size 9', async () => {
    listPokemonMock.mockResolvedValue(pageResult(ninePokemon(), { totalPages: 2, totalElements: 18 }))

    renderCatalog('/')
    await waitFor(() => expect(listPokemonMock).toHaveBeenCalled())
    expect(listPokemonMock).toHaveBeenCalledWith(
      { page: 1, size: 9 },
      expect.any(AbortSignal),
    )

    listPokemonMock.mockClear()
    renderCatalog('/?page=1')
    await waitFor(() => expect(listPokemonMock).toHaveBeenCalled())
    expect(listPokemonMock).toHaveBeenCalledWith(
      { page: 1, size: 9 },
      expect.any(AbortSignal),
    )
  })

  it('renders nine cards with sprite, metadata, abilities, and detail links', async () => {
    const first = pokemon({
      nationalNumber: 1,
      name: 'Bulbasaur',
      abilities: [
        { name: 'overgrow', hidden: false },
        { name: 'chlorophyll', hidden: true },
      ],
    })
    listPokemonMock.mockResolvedValue(
      pageResult(
        [first, ...ninePokemon().slice(1)],
        { totalPages: 2, totalElements: 18 },
      ),
    )

    renderCatalog('/')

    await waitFor(() => {
      expect(screen.getByRole('link', { name: /bulbasaur/i })).toBeInTheDocument()
    })

    const card = screen.getByRole('link', { name: /bulbasaur/i })
    expect(card).toHaveAttribute('href', '/pokemon/1')

    const img = within(card).getByRole('img', { name: 'Bulbasaur' })
    expect(img).toHaveAttribute('src', 'https://example.com/1.png')
    expect(img).toHaveAttribute('width')
    expect(img).toHaveAttribute('height')
    expect(img).toHaveAttribute('loading', 'lazy')

    expect(within(card).getByText('#0001')).toBeInTheDocument()
    expect(within(card).getByText('Seed Pokémon')).toBeInTheDocument()
    expect(within(card).getByText('6.9 kg')).toBeInTheDocument()
    expect(within(card).getByText('Overgrow')).toBeInTheDocument()
    expect(within(card).getByText('Chlorophyll')).toBeInTheDocument()
    expect(within(card).getByText('H', { selector: '[aria-hidden="true"]' })).toBeInTheDocument()
    expect(within(card).getByText('Hidden ability')).toHaveClass('sr-only')

    const heading = screen.getByRole('heading', {
      level: 1,
      name: 'The Pokémon catalog',
    })
    expect(heading).toHaveClass('text-display')
    expect(heading).toHaveClass('text-foreground')

    const abilityBadge = within(card).getByText('Overgrow').closest('[data-slot="badge"]')
    expect(abilityBadge).toHaveClass('text-caption')

    expect(img).toHaveStyle({ imageRendering: 'pixelated' })
    expect(screen.getByText(/0001–0009 of/i)).toBeInTheDocument()
    expect(screen.getByText(/H = Hidden ability/i)).toBeInTheDocument()

    expect(screen.getAllByRole('link', { name: /pokemon\d+/i })).toHaveLength(8)
  })

  it('shows loading skeletons before data arrives', async () => {
    let resolvePage!: (value: PokemonPage) => void
    listPokemonMock.mockImplementation(
      () =>
        new Promise<PokemonPage>((resolve) => {
          resolvePage = resolve
        }),
    )

    renderCatalog('/')

    expect(screen.getByTestId('catalog-skeleton')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /bulbasaur/i })).not.toBeInTheDocument()

    await act(async () => {
      resolvePage(pageResult([pokemon()]))
    })

    await waitFor(() => {
      expect(screen.getByRole('link', { name: /bulbasaur/i })).toBeInTheDocument()
    })
  })

  it('keeps previous grid visible while loading page 2 (keepPreviousData)', async () => {
    const user = userEvent.setup()
    listPokemonMock.mockResolvedValueOnce(
      pageResult(ninePokemon(), { page: 1, totalPages: 2, totalElements: 18 }),
    )

    let resolvePage2!: (value: PokemonPage) => void
    listPokemonMock.mockImplementationOnce(
      () =>
        new Promise<PokemonPage>((resolve) => {
          resolvePage2 = resolve
        }),
    )

    const router = renderCatalog('/')
    await waitFor(() => {
      expect(screen.getByRole('link', { name: /pokemon1/i })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('link', { name: /^2$/ }))

    await waitFor(() => {
      expect(router.state.location.search).toBe('?page=2')
    })
    expect(screen.getByRole('link', { name: /pokemon1/i })).toBeInTheDocument()

    await act(async () => {
      resolvePage2(
        pageResult(
          [
            pokemon({
              nationalNumber: 10,
              name: 'Page2Mon',
              spriteUrl: 'https://example.com/10.png',
            }),
          ],
          { page: 2, totalPages: 2, totalElements: 18 },
        ),
      )
    })

    await waitFor(() => {
      expect(screen.getByRole('link', { name: /page2mon/i })).toBeInTheDocument()
    })
  })

  it('replaces invalid page with page=1 and does not call API with invalid value', async () => {
    listPokemonMock.mockResolvedValue(pageResult([pokemon()]))

    for (const entry of ['/?page=0', '/?page=-1', '/?page=abc']) {
      listPokemonMock.mockClear()
      const router = renderCatalog(entry)

      await waitFor(() => {
        expect(router.state.location.search).toBe('?page=1')
      })
      await waitFor(() => expect(listPokemonMock).toHaveBeenCalled())
      expect(listPokemonMock).toHaveBeenCalledWith(
        { page: 1, size: 9 },
        expect.any(AbortSignal),
      )
      expect(listPokemonMock.mock.calls.every(([query]) => query.page === 1)).toBe(
        true,
      )
    }
  })

  it('shows empty state with link to page 1 when API returns beyond-last 400', async () => {
    listPokemonMock.mockRejectedValue(
      new ApiError({
        status: 400,
        type: 'urn:problem-type:bad-request',
        title: 'Bad Request',
        detail: 'page 5 is beyond last page 1',
      }),
    )

    renderCatalog('/?page=5')

    await waitFor(() => {
      expect(screen.getByText('No records on this page')).toBeInTheDocument()
    })
    expect(screen.queryByText(/something went wrong/i)).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Back to page 1' })).toHaveAttribute(
      'href',
      '/?page=1',
    )
  })

  it('shows catalog unavailable on 502 with Retry that refetches', async () => {
    const user = userEvent.setup()
    const unavailable = new ApiError({
      status: 502,
      type: 'urn:problem-type:bad-gateway',
      title: 'Bad Gateway',
      detail: 'Upstream temporarily unavailable',
    })
    // QueryClient retries 502 once, then the UI Retry refetches.
    listPokemonMock
      .mockRejectedValueOnce(unavailable)
      .mockRejectedValueOnce(unavailable)
      .mockResolvedValueOnce(pageResult([pokemon()]))

    renderCatalog('/')

    await waitFor(() => {
      expect(screen.getByText(/catalog unavailable/i)).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: /retry|try again/i }))

    await waitFor(() => {
      expect(screen.getByRole('link', { name: /bulbasaur/i })).toBeInTheDocument()
    })
    expect(listPokemonMock).toHaveBeenCalledTimes(3)
  })

  it('shows connection-error state on network failure with Retry', async () => {
    const user = userEvent.setup()
    const network = new ApiError({
      status: 0,
      type: 'network',
      title: 'Network Error',
      detail: 'Network error. Try again.',
    })
    listPokemonMock
      .mockRejectedValueOnce(network)
      .mockRejectedValueOnce(network)
      .mockResolvedValueOnce(pageResult([pokemon()]))

    renderCatalog('/')

    await waitFor(() => {
      expect(screen.getByText('Connection error')).toBeInTheDocument()
    })
    expect(screen.queryByText(/catalog unavailable/i)).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /retry|try again/i }))

    await waitFor(() => {
      expect(screen.getByRole('link', { name: /bulbasaur/i })).toBeInTheDocument()
    })
  })
})
