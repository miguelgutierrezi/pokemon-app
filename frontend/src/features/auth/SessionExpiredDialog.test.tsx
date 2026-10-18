import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { ApiError } from '@/api/api-error'
import type { LocalPokemon } from '@/api/types'
import { AppProviders } from '@/app/providers'
import { routerConfig } from '@/app/routes'
import {
  openSessionExpired,
  resetSessionExpiredState,
} from '@/features/auth/session-expired'
import { SESSION_KEY } from '@/lib/session'

vi.mock('@/api/collection', () => ({
  listCollection: vi.fn().mockResolvedValue({
    content: [],
    page: 1,
    size: 9,
    totalElements: 0,
    totalPages: 0,
  }),
  getLocalPokemonById: vi.fn(),
  updateLocalPokemon: vi.fn(),
  deleteLocalPokemon: vi.fn(),
  importPokemon: vi.fn(),
}))

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

import { getLocalPokemonById, updateLocalPokemon } from '@/api/collection'
import { getPokemonById } from '@/api/pokemon'

const getLocalMock = vi.mocked(getLocalPokemonById)
const updateMock = vi.mocked(updateLocalPokemon)
const getPokemonMock = vi.mocked(getPokemonById)

function savedPokemon(overrides: Partial<LocalPokemon> = {}): LocalPokemon {
  return {
    nationalNumber: 1,
    name: 'Bulbasaur',
    types: ['grass'],
    heightM: 0.7,
    weightKg: 6.9,
    category: 'Seed Pokémon',
    generation: 'generation-i',
    imageUrl: 'https://example.com/1.png',
    syncedAt: '2026-01-01T00:00:00Z',
    localizedName: null,
    location: null,
    tags: [],
    version: 2,
    ...overrides,
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

function renderApp(initialEntry: string) {
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

describe('SessionExpiredDialog', () => {
  beforeEach(() => {
    resetSessionExpiredState()
    localStorage.clear()
    getLocalMock.mockReset()
    updateMock.mockReset()
    getPokemonMock.mockReset()
    getLocalMock.mockResolvedValue(savedPokemon())
  })

  it('shows title, route context, and Sign in again with sanitised redirect', async () => {
    const user = userEvent.setup()
    const router = renderApp('/collection?page=2')

    openSessionExpired()

    const dialog = await screen.findByRole('alertdialog', {
      name: /Your session has expired/i,
    })
    expect(within(dialog).getByText('Collection')).toBeInTheDocument()
    expect(
      within(dialog).getByText(/explore the public catalog without signing in/i),
    ).toBeInTheDocument()

    await user.click(
      within(dialog).getByRole('button', { name: /Sign in again/i }),
    )

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/login')
    })
    expect(router.state.location.search).toContain('reason=expired')
    expect(router.state.location.search).toContain(
      `redirect=${encodeURIComponent('/collection?page=2')}`,
    )
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
  })

  it('Browse catalog goes to / and closes the dialog', async () => {
    const user = userEvent.setup()
    const router = renderApp('/collection')

    openSessionExpired()
    const dialog = await screen.findByRole('alertdialog')

    await user.click(
      within(dialog).getByRole('button', { name: /Browse catalog/i }),
    )

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/')
    })
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
  })

  it('X and Escape close the dialog without navigating', async () => {
    const user = userEvent.setup()
    const router = renderApp('/collection')

    openSessionExpired()
    const dialog = await screen.findByRole('alertdialog')

    await user.click(
      within(dialog).getByRole('button', { name: /Dismiss dialog/i }),
    )
    await waitFor(() => {
      expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
    })
    expect(router.state.location.pathname).toBe('/collection')

    openSessionExpired()
    await screen.findByRole('alertdialog')
    await user.keyboard('{Escape}')
    await waitFor(() => {
      expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
    })
    expect(router.state.location.pathname).toBe('/collection')
  })

  it('protected-write 401 from edit opens dialog with entity context; no success; session cleared', async () => {
    seedSession()
    const user = userEvent.setup()
    updateMock.mockRejectedValue(
      new ApiError({
        status: 401,
        type: 'urn:problem-type:unauthorized',
        title: 'Unauthorized',
        detail: 'Missing or invalid access token',
      }),
    )

    const router = renderApp('/collection/1')
    await waitFor(() => {
      expect(
        screen.getByRole('button', { name: /Save changes/i }),
      ).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: /Save changes/i }))

    const dialog = await screen.findByRole('alertdialog', {
      name: /Your session has expired/i,
    })
    expect(
      within(dialog).getByText('Edit Pokémon / Bulbasaur'),
    ).toBeInTheDocument()
    expect(
      within(dialog).getByText(/continue saving Bulbasaur/i),
    ).toBeInTheDocument()
    expect(screen.queryByText(/updated in the collection/i)).not.toBeInTheDocument()
    expect(localStorage.getItem(SESSION_KEY)).toBeNull()
    expect(router.state.location.pathname).toBe('/collection/1')
  })
})
