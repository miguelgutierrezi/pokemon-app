import { render, screen, waitFor, within } from '@testing-library/react'
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
import { resetSessionExpiredState } from '@/features/auth/session-expired'
import { SESSION_KEY } from '@/lib/session'

vi.mock('@/api/pokemon', () => ({
  listPokemon: vi.fn().mockResolvedValue({
    content: [],
    page: 1,
    size: 9,
    totalElements: 0,
    totalPages: 0,
  }),
  getPokemonById: vi.fn(),
}))

vi.mock('@/api/collection', () => ({
  getLocalPokemonById: vi.fn(),
  importPokemon: vi.fn(),
}))

vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  register: vi.fn(),
}))

import { getLocalPokemonById, importPokemon } from '@/api/collection'
import { getPokemonById } from '@/api/pokemon'

const getPokemonByIdMock = vi.mocked(getPokemonById)
const getLocalPokemonByIdMock = vi.mocked(getLocalPokemonById)
const importPokemonMock = vi.mocked(importPokemon)

function linearEvolution(): EvolutionNode {
  return {
    nationalNumber: 1,
    name: 'Bulbasaur',
    imageUrl: 'https://example.com/sprite/1.png',
    isCurrent: true,
    children: [],
  }
}

function bulbasaurDetail(): PokemonDetail {
  return {
    nationalNumber: 1,
    name: 'Bulbasaur',
    category: 'Seed Pokémon',
    types: ['grass', 'poison'],
    heightM: 0.7,
    weightKg: 6.9,
    abilities: [{ name: 'overgrow', hidden: false }],
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
    flavorText: { text: 'A strange seed.', version: 'red' },
    evolution: linearEvolution(),
  }
}

function savedLocal(): LocalPokemon {
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
    localizedName: null,
    location: null,
    tags: [],
    version: 1,
  }
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

function renderDetail(initialEntry = '/pokemon/1') {
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

async function openConfirmDialog(user: ReturnType<typeof userEvent.setup>) {
  await waitFor(() => {
    expect(
      screen.getByRole('button', { name: /Add to collection/i }),
    ).toBeInTheDocument()
  })
  await user.click(screen.getByRole('button', { name: /Add to collection/i }))
  await waitFor(() => {
    expect(
      screen.getByRole('alertdialog', { name: /Add to the collection/i }),
    ).toBeInTheDocument()
  })
}

describe('PokemonDetailPage import flow', () => {
  beforeEach(() => {
    resetSessionExpiredState()
    getPokemonByIdMock.mockReset()
    getLocalPokemonByIdMock.mockReset()
    importPokemonMock.mockReset()
    localStorage.clear()
    getPokemonByIdMock.mockResolvedValue(bulbasaurDetail())
    getLocalPokemonByIdMock.mockRejectedValue(
      new ApiError({
        status: 404,
        type: 'urn:problem-type:not-found',
        title: 'Not Found',
        detail: 'Local Pokémon not found',
      }),
    )
  })

  it('signed out: Add to collection navigates to login with redirect, no dialog or request', async () => {
    const router = renderDetail('/pokemon/1')
    const user = userEvent.setup()

    await waitFor(() => {
      expect(
        screen.getByRole('button', { name: /Add to collection/i }),
      ).toBeInTheDocument()
    })
    await user.click(screen.getByRole('button', { name: /Add to collection/i }))

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/login')
    })
    expect(router.state.location.search).toBe('?redirect=%2Fpokemon%2F1')
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
    expect(importPokemonMock).not.toHaveBeenCalled()
  })

  it('signed in: confirm → progress → success with View in the collection link', async () => {
    seedSession()
    let resolveImport!: (value: LocalPokemon) => void
    importPokemonMock.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveImport = resolve
        }),
    )

    renderDetail('/pokemon/1')
    const user = userEvent.setup()
    await openConfirmDialog(user)

    const dialog = screen.getByRole('alertdialog')
    expect(
      within(dialog).getByText(/Add to the collection\?/i),
    ).toBeInTheDocument()
    expect(within(dialog).getByText('Bulbasaur')).toBeInTheDocument()
    expect(within(dialog).getByText(/#0001/)).toBeInTheDocument()
    expect(within(dialog).getByText('Seed Pokémon')).toBeInTheDocument()
    expect(within(dialog).getByText(/shared collection/i)).toBeInTheDocument()
    expect(
      within(dialog).queryByText(/your account/i),
    ).not.toBeInTheDocument()

    await user.click(within(dialog).getByRole('button', { name: /^Cancel$/i }))
    await waitFor(() => {
      expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
    })
    expect(importPokemonMock).not.toHaveBeenCalled()

    await openConfirmDialog(user)
    const confirmDialog = screen.getByRole('alertdialog')
    await user.click(
      within(confirmDialog).getByRole('button', {
        name: /Add to collection|Confirm/i,
      }),
    )

    await waitFor(() => {
      expect(importPokemonMock).toHaveBeenCalled()
    })
    expect(importPokemonMock).toHaveBeenCalledWith(expect.anything(), {
      nationalNumber: 1,
    })

    const progressDialog = screen.getByRole('alertdialog')
    expect(within(progressDialog).getByText('Please wait')).toBeInTheDocument()
    expect(
      within(progressDialog).getByRole('button', { name: /Saving/i }),
    ).toBeDisabled()

    getLocalPokemonByIdMock.mockResolvedValue(savedLocal())
    resolveImport(savedLocal())

    await waitFor(() => {
      expect(
        screen.getByRole('link', { name: /View in the collection/i }),
      ).toHaveAttribute('href', '/collection/1')
    })
    expect(screen.getByRole('button', { name: /^Close$/i })).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /^Close$/i }))
    await waitFor(() => {
      expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
    })
    await waitFor(() => {
      expect(screen.getByText(/Already in the collection/i)).toBeInTheDocument()
    })
  })

  it('409 shows Already in the collection dialog with a link to the record', async () => {
    seedSession()
    importPokemonMock.mockRejectedValue(
      new ApiError({
        status: 409,
        type: 'urn:problem-type:conflict',
        title: 'Conflict',
        detail: 'already in local collection',
      }),
    )
    getLocalPokemonByIdMock
      .mockRejectedValueOnce(
        new ApiError({
          status: 404,
          type: 'urn:problem-type:not-found',
          title: 'Not Found',
          detail: 'Local Pokémon not found',
        }),
      )
      .mockResolvedValue(savedLocal())

    renderDetail('/pokemon/1')
    const user = userEvent.setup()
    await openConfirmDialog(user)
    await user.click(
      within(screen.getByRole('alertdialog')).getByRole('button', {
        name: /Add to collection|Confirm/i,
      }),
    )

    await waitFor(() => {
      expect(
        screen.getByRole('heading', { name: /Already in the collection/i }),
      ).toBeInTheDocument()
    })
    expect(
      screen.getByRole('link', { name: /View in the collection|Edit/i }),
    ).toHaveAttribute('href', '/collection/1')
  })

  it('502 shows an error with Retry', async () => {
    seedSession()
    importPokemonMock.mockRejectedValue(
      new ApiError({
        status: 502,
        type: 'urn:problem-type:bad-gateway',
        title: 'Bad Gateway',
        detail: 'Upstream unavailable',
      }),
    )

    renderDetail('/pokemon/1')
    const user = userEvent.setup()
    await openConfirmDialog(user)
    await user.click(
      within(screen.getByRole('alertdialog')).getByRole('button', {
        name: /Add to collection|Confirm/i,
      }),
    )

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Retry/i })).toBeInTheDocument()
    })
    expect(screen.queryByText(/View in the collection/i)).not.toBeInTheDocument()
  })

  it('401 closes the import dialog and opens session-expired without success', async () => {
    seedSession()
    importPokemonMock.mockRejectedValue(
      new ApiError({
        status: 401,
        type: 'urn:problem-type:unauthorized',
        title: 'Unauthorized',
        detail: 'Token expired',
      }),
    )

    renderDetail('/pokemon/1')
    const user = userEvent.setup()
    await openConfirmDialog(user)
    await user.click(
      within(screen.getByRole('alertdialog')).getByRole('button', {
        name: /Add to collection|Confirm/i,
      }),
    )

    const expired = await screen.findByRole('alertdialog', {
      name: /Your session has expired/i,
    })
    expect(
      within(expired).getByText('Pokémon details / Bulbasaur'),
    ).toBeInTheDocument()
    expect(screen.queryByText(/View in the collection/i)).not.toBeInTheDocument()
    expect(screen.queryByText(/Add to the collection\?/i)).not.toBeInTheDocument()
  })
})
