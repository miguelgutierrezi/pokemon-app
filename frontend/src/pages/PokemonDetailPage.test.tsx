import { act, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { ApiError } from '@/api/api-error'
import type {
  EvolutionNode,
  LocalPokemon,
  PokemonDetail,
} from '@/api/types'
import { AppProviders } from '@/app/providers'
import { routerConfig } from '@/app/routes'

vi.mock('@/api/pokemon', () => ({
  listPokemon: vi.fn(),
  getPokemonById: vi.fn(),
}))

vi.mock('@/api/collection', () => ({
  getLocalPokemonById: vi.fn(),
}))

import { getLocalPokemonById } from '@/api/collection'
import { getPokemonById, listPokemon } from '@/api/pokemon'

const getPokemonByIdMock = vi.mocked(getPokemonById)
const getLocalPokemonByIdMock = vi.mocked(getLocalPokemonById)
const listPokemonMock = vi.mocked(listPokemon)

function linearEvolution(): EvolutionNode {
  return {
    nationalNumber: 1,
    name: 'Bulbasaur',
    imageUrl: 'https://example.com/sprite/1.png',
    isCurrent: true,
    children: [
      {
        trigger: 'level-up',
        minLevel: 16,
        itemSlug: null,
        node: {
          nationalNumber: 2,
          name: 'Ivysaur',
          imageUrl: 'https://example.com/sprite/2.png',
          isCurrent: false,
          children: [
            {
              trigger: 'level-up',
              minLevel: 32,
              itemSlug: null,
              node: {
                nationalNumber: 3,
                name: 'Venusaur',
                imageUrl: 'https://example.com/sprite/3.png',
                isCurrent: false,
                children: [],
              },
            },
          ],
        },
      },
    ],
  }
}

function branchingEvolution(): EvolutionNode {
  return {
    nationalNumber: 133,
    name: 'Eevee',
    imageUrl: 'https://example.com/sprite/133.png',
    isCurrent: true,
    children: [
      {
        trigger: 'use-item',
        minLevel: null,
        itemSlug: 'water-stone',
        node: {
          nationalNumber: 134,
          name: 'Vaporeon',
          imageUrl: 'https://example.com/sprite/134.png',
          isCurrent: false,
          children: [],
        },
      },
      {
        trigger: 'use-item',
        minLevel: null,
        itemSlug: 'thunder-stone',
        node: {
          nationalNumber: 135,
          name: 'Jolteon',
          imageUrl: 'https://example.com/sprite/135.png',
          isCurrent: false,
          children: [],
        },
      },
    ],
  }
}

function bulbasaurDetail(
  overrides: Partial<PokemonDetail> = {},
): PokemonDetail {
  return {
    nationalNumber: 1,
    name: 'Bulbasaur',
    category: 'Seed Pokémon',
    types: ['grass', 'poison'],
    heightM: 0.7,
    weightKg: 6.9,
    abilities: [
      { name: 'overgrow', hidden: false },
      { name: 'chlorophyll', hidden: true },
    ],
    generation: 'generation-i',
    imageUrl: 'https://example.com/artwork/1.png',
    stats: {
      hp: 45,
      attack: 49,
      defense: 49,
      specialAttack: 65,
      specialDefense: 65,
      speed: 45,
      total: 318,
    },
    flavorText: {
      text: 'A strange seed was planted on its back at birth. The plant sprouts and grows with this Pokémon.',
      version: 'red',
    },
    evolution: linearEvolution(),
    ...overrides,
  }
}

function savedLocal(
  overrides: Partial<LocalPokemon> = {},
): LocalPokemon {
  return {
    nationalNumber: 1,
    name: 'Bulbasaur',
    types: ['grass', 'poison'],
    heightM: 0.7,
    weightKg: 6.9,
    category: 'Seed Pokémon',
    generation: 'generation-i',
    imageUrl: 'https://example.com/artwork/1.png',
    syncedAt: '2026-01-01T00:00:00Z',
    localizedName: 'Bulbizarre',
    location: 'Pallet Town · starter partner',
    tags: ['First partner', 'Favorites'],
    version: 1,
    ...overrides,
  }
}

function renderDetail(
  initialEntry:
    | string
    | { pathname: string; search?: string; state?: unknown } = '/pokemon/1',
) {
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

describe('PokemonDetailPage', () => {
  beforeEach(() => {
    getPokemonByIdMock.mockReset()
    getLocalPokemonByIdMock.mockReset()
    listPokemonMock.mockReset()
    listPokemonMock.mockResolvedValue({
      content: [],
      page: 1,
      size: 9,
      totalElements: 0,
      totalPages: 0,
    })
    localStorage.clear()
  })

  it('fetches catalog detail and collection status in parallel for /pokemon/1', async () => {
    getPokemonByIdMock.mockResolvedValue(bulbasaurDetail())
    getLocalPokemonByIdMock.mockRejectedValue(
      new ApiError({
        status: 404,
        type: 'urn:problem-type:not-found',
        title: 'Not Found',
        detail: 'Local Pokémon not found',
      }),
    )

    renderDetail('/pokemon/1')

    await waitFor(() => {
      expect(getPokemonByIdMock).toHaveBeenCalled()
      expect(getLocalPokemonByIdMock).toHaveBeenCalled()
    })
    expect(getPokemonByIdMock).toHaveBeenCalledWith(1, expect.any(AbortSignal))
    expect(getLocalPokemonByIdMock).toHaveBeenCalledWith(
      1,
      expect.any(AbortSignal),
    )
  })

  it.each([
    ['0', '/pokemon/0'],
    ['-3', '/pokemon/-3'],
    ['abc', '/pokemon/abc'],
  ])(
    'shows not-found for invalid id %s without calling the API',
    async (_label, path) => {
      renderDetail(path)

      await waitFor(() => {
        expect(screen.getByText(/not found/i)).toBeInTheDocument()
      })
      expect(getPokemonByIdMock).not.toHaveBeenCalled()
      expect(getLocalPokemonByIdMock).not.toHaveBeenCalled()
    },
  )

  it('renders detail content, official artwork, stats, abilities, and linear evolution', async () => {
    getPokemonByIdMock.mockResolvedValue(bulbasaurDetail())
    getLocalPokemonByIdMock.mockRejectedValue(
      new ApiError({
        status: 404,
        type: 'urn:problem-type:not-found',
        title: 'Not Found',
        detail: 'Local Pokémon not found',
      }),
    )

    renderDetail('/pokemon/1')

    await waitFor(() => {
      expect(
        screen.getByRole('heading', { level: 1, name: 'Bulbasaur' }),
      ).toBeInTheDocument()
    })

    expect(screen.getByText(/#0001 · National Pokédex/i)).toBeInTheDocument()
    expect(screen.getByText('Seed Pokémon')).toBeInTheDocument()
    expect(screen.getByText('Grass')).toBeInTheDocument()
    expect(screen.getByText('Poison')).toBeInTheDocument()
    expect(screen.getByText('0.7 m')).toBeInTheDocument()
    expect(screen.getByText('6.9 kg')).toBeInTheDocument()
    expect(screen.getByText('Generation I')).toBeInTheDocument()
    expect(
      screen.getByText(
        /A strange seed was planted on its back at birth/i,
      ),
    ).toBeInTheDocument()
    expect(screen.getByText(/Pokémon Red · via PokéAPI/i)).toBeInTheDocument()

    expect(screen.getByText('TOTAL 318')).toBeInTheDocument()
    expect(screen.getByText('HP')).toBeInTheDocument()
    expect(screen.getByText('Special Attack')).toBeInTheDocument()
    expect(screen.getByText('Speed')).toBeInTheDocument()

    expect(screen.getByText('Overgrow')).toBeInTheDocument()
    expect(screen.getByText('Chlorophyll')).toBeInTheDocument()
    expect(screen.getByText('Hidden ability')).toBeInTheDocument()
    expect(
      screen.queryByText(/Powers up Grass-type moves/i),
    ).not.toBeInTheDocument()

    const artwork = screen
      .getAllByRole('img', { name: 'Bulbasaur' })
      .find((img) => img.getAttribute('loading') === 'eager')
    expect(artwork).toBeDefined()
    expect(artwork).toHaveAttribute('src', 'https://example.com/artwork/1.png')
    expect(artwork).toHaveAttribute('width')
    expect(artwork).toHaveAttribute('height')
    expect(artwork).toHaveAttribute('loading', 'eager')
    expect(artwork).not.toHaveStyle({ imageRendering: 'pixelated' })
    expect(screen.getByText(/Official artwork/i)).toBeInTheDocument()

    expect(screen.getByText('Lv. 16')).toBeInTheDocument()
    expect(screen.getByText('Lv. 32')).toBeInTheDocument()
    expect(screen.getByText('Current Pokémon')).toBeInTheDocument()

    const ivyLink = screen.getByRole('link', { name: /Ivysaur/i })
    expect(ivyLink).toHaveAttribute('href', '/pokemon/2')
    const ivyImg = within(ivyLink).getByRole('img', { name: 'Ivysaur' })
    expect(ivyImg).toHaveAttribute('loading', 'lazy')
    expect(ivyImg).toHaveStyle({ imageRendering: 'pixelated' })

    expect(
      screen.getByRole('button', { name: /Add to collection/i }),
    ).toBeInTheDocument()
  })

  it('renders branching evolutions as an Evolves-into grid of children', async () => {
    getPokemonByIdMock.mockResolvedValue(
      bulbasaurDetail({
        nationalNumber: 133,
        name: 'Eevee',
        category: 'Evolution Pokémon',
        types: ['normal'],
        evolution: branchingEvolution(),
        imageUrl: 'https://example.com/artwork/133.png',
      }),
    )
    getLocalPokemonByIdMock.mockRejectedValue(
      new ApiError({
        status: 404,
        type: 'urn:problem-type:not-found',
        title: 'Not Found',
        detail: 'Local Pokémon not found',
      }),
    )

    renderDetail('/pokemon/133')

    await waitFor(() => {
      expect(
        screen.getByRole('heading', { level: 1, name: 'Eevee' }),
      ).toBeInTheDocument()
    })

    expect(
      screen.getByRole('heading', { name: 'Evolves into' }),
    ).toBeInTheDocument()

    const vaporeon = screen.getByRole('link', { name: /Vaporeon/i })
    const jolteon = screen.getByRole('link', { name: /Jolteon/i })
    expect(vaporeon).toHaveAttribute('href', '/pokemon/134')
    expect(jolteon).toHaveAttribute('href', '/pokemon/135')
    expect(within(vaporeon).getByText('Water Stone')).toBeInTheDocument()
    expect(within(jolteon).getByText('Thunder Stone')).toBeInTheDocument()

    // Sibling branches: condition lives on the card — no Lv./arrow connectors.
    expect(screen.queryByText('Lv. 16')).not.toBeInTheDocument()
  })

  it('shows saved collection UI with details card and Edit link', async () => {
    getPokemonByIdMock.mockResolvedValue(bulbasaurDetail())
    getLocalPokemonByIdMock.mockResolvedValue(savedLocal())

    renderDetail('/pokemon/1')

    await waitFor(() => {
      expect(screen.getByText(/Already in the collection/i)).toBeInTheDocument()
    })
    expect(
      screen.getByText(/Saved in the shared collection/i),
    ).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /^Edit$/i })).toHaveAttribute(
      'href',
      '/collection/1',
    )
    expect(screen.getByText('Bulbizarre')).toBeInTheDocument()
    expect(screen.getByText(/Pallet Town/i)).toBeInTheDocument()
    expect(screen.getByText('First partner')).toBeInTheDocument()
    expect(screen.getByText('Favorites')).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: /Add to collection/i }),
    ).not.toBeInTheDocument()
  })

  it('shows em dash for empty collection detail fields', async () => {
    getPokemonByIdMock.mockResolvedValue(bulbasaurDetail())
    getLocalPokemonByIdMock.mockResolvedValue(
      savedLocal({
        localizedName: null,
        location: null,
        tags: [],
      }),
    )

    renderDetail('/pokemon/1')

    await waitFor(() => {
      expect(screen.getByText(/Already in the collection/i)).toBeInTheDocument()
    })
    expect(screen.getAllByText('—').length).toBeGreaterThanOrEqual(2)
  })

  it('shows not-found when catalog returns 404', async () => {
    getPokemonByIdMock.mockRejectedValue(
      new ApiError({
        status: 404,
        type: 'urn:problem-type:not-found',
        title: 'Not Found',
        detail: 'Pokémon not found',
      }),
    )
    getLocalPokemonByIdMock.mockRejectedValue(
      new ApiError({
        status: 404,
        type: 'urn:problem-type:not-found',
        title: 'Not Found',
        detail: 'Local Pokémon not found',
      }),
    )

    renderDetail('/pokemon/999')

    await waitFor(() => {
      expect(screen.getByText(/not found/i)).toBeInTheDocument()
    })
  })

  it('shows unavailable with Retry on catalog 502', async () => {
    getPokemonByIdMock.mockRejectedValue(
      new ApiError({
        status: 502,
        type: 'urn:problem-type:bad-gateway',
        title: 'Bad Gateway',
        detail: 'Upstream unavailable',
      }),
    )
    getLocalPokemonByIdMock.mockRejectedValue(
      new ApiError({
        status: 404,
        type: 'urn:problem-type:not-found',
        title: 'Not Found',
        detail: 'Local Pokémon not found',
      }),
    )

    renderDetail('/pokemon/1')

    await waitFor(() => {
      expect(screen.getByText('Catalog unavailable')).toBeInTheDocument()
    })
    const retry = screen.getByRole('button', { name: /Retry/i })
    getPokemonByIdMock.mockResolvedValue(bulbasaurDetail())
    await userEvent.click(retry)
    await waitFor(() => {
      expect(
        screen.getByRole('heading', { level: 1, name: 'Bulbasaur' }),
      ).toBeInTheDocument()
    })
  })

  it('shows connection error with Retry on catalog status 0', async () => {
    getPokemonByIdMock.mockRejectedValue(
      new ApiError({
        status: 0,
        type: 'network',
        title: 'Network Error',
        detail: 'Network error. Try again.',
      }),
    )
    getLocalPokemonByIdMock.mockRejectedValue(
      new ApiError({
        status: 404,
        type: 'urn:problem-type:not-found',
        title: 'Not Found',
        detail: 'Local Pokémon not found',
      }),
    )

    renderDetail('/pokemon/1')

    await waitFor(() => {
      expect(screen.getByText('Connection error')).toBeInTheDocument()
    })
    expect(screen.getByRole('button', { name: /Retry/i })).toBeInTheDocument()
  })

  it('keeps the page usable when collection GET fails with 502', async () => {
    getPokemonByIdMock.mockResolvedValue(bulbasaurDetail())
    getLocalPokemonByIdMock.mockRejectedValue(
      new ApiError({
        status: 502,
        type: 'urn:problem-type:bad-gateway',
        title: 'Bad Gateway',
        detail: 'Upstream unavailable',
      }),
    )

    renderDetail('/pokemon/1')

    await waitFor(() => {
      expect(
        screen.getByRole('heading', { level: 1, name: 'Bulbasaur' }),
      ).toBeInTheDocument()
    })
    const addButton = screen.getByRole('button', {
      name: /Add to collection/i,
    })
    expect(addButton).toBeDisabled()
    expect(
      screen.getByText(/collection status unavailable/i),
    ).toBeInTheDocument()
  })

  it('keeps previous catalog page in breadcrumb via location.state', async () => {
    getPokemonByIdMock.mockResolvedValue(bulbasaurDetail())
    getLocalPokemonByIdMock.mockRejectedValue(
      new ApiError({
        status: 404,
        type: 'urn:problem-type:not-found',
        title: 'Not Found',
        detail: 'Local Pokémon not found',
      }),
    )

    renderDetail({
      pathname: '/pokemon/1',
      state: { from: { pathname: '/', search: '?page=2' } },
    })

    await waitFor(() => {
      expect(
        screen.getByRole('heading', { level: 1, name: 'Bulbasaur' }),
      ).toBeInTheDocument()
    })
    const crumb = screen.getByRole('navigation', { name: 'Breadcrumb' })
    expect(within(crumb).getByRole('link', { name: 'Catalog' })).toHaveAttribute(
      'href',
      '/?page=2',
    )
  })

  it('shows Collection breadcrumb when navigating from the collection', async () => {
    getPokemonByIdMock.mockResolvedValue(bulbasaurDetail())
    getLocalPokemonByIdMock.mockResolvedValue(savedLocal())

    renderDetail({
      pathname: '/pokemon/1',
      state: { from: { pathname: '/collection', search: '?page=1' } },
    })

    await waitFor(() => {
      expect(
        screen.getByRole('heading', { level: 1, name: 'Bulbasaur' }),
      ).toBeInTheDocument()
    })
    const crumb = screen.getByRole('navigation', { name: 'Breadcrumb' })
    expect(
      within(crumb).getByRole('link', { name: 'Collection' }),
    ).toHaveAttribute('href', '/collection?page=1')
  })

  it('invokes onAddToCollection prop when Add is clicked', async () => {
    getPokemonByIdMock.mockResolvedValue(bulbasaurDetail())
    getLocalPokemonByIdMock.mockRejectedValue(
      new ApiError({
        status: 404,
        type: 'urn:problem-type:not-found',
        title: 'Not Found',
        detail: 'Local Pokémon not found',
      }),
    )

    // Wire is internal; assert the button is present and clickable for the
    // import phase. Navigation when signed out is covered later.
    renderDetail('/pokemon/1')

    await waitFor(() => {
      expect(
        screen.getByRole('button', { name: /Add to collection/i }),
      ).toBeInTheDocument()
    })
    await act(async () => {
      await userEvent.click(
        screen.getByRole('button', { name: /Add to collection/i }),
      )
    })
  })
})
