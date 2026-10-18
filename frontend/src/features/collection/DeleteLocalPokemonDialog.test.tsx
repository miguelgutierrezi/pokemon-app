import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { ApiError } from '@/api/api-error'
import type { LocalPokemon } from '@/api/types'
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

import { deleteLocalPokemon, listCollection } from '@/api/collection'

const listMock = vi.mocked(listCollection)
const deleteMock = vi.mocked(deleteLocalPokemon)

function entry(): LocalPokemon {
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
    localizedName: 'Bulbizarre',
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

function renderCollection() {
  const router = createMemoryRouter(routerConfig, {
    initialEntries: ['/collection'],
  })
  render(
    <AppProviders>
      <RouterProvider router={router} />
    </AppProviders>,
  )
  return router
}

describe('RemoveDialog from collection list', () => {
  beforeEach(() => {
    listMock.mockReset()
    deleteMock.mockReset()
    localStorage.clear()
    listMock.mockResolvedValue({
      content: [entry()],
      page: 1,
      size: 9,
      totalElements: 1,
      totalPages: 1,
    })
  })

  it('signed-in Remove opens dialog with title and description; cancel keeps record', async () => {
    seedSession()
    const user = userEvent.setup()
    renderCollection()

    await waitFor(() => {
      expect(screen.getByRole('article', { name: /bulbasaur/i })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: /^Remove$/i }))
    const dialog = await screen.findByRole('alertdialog', {
      name: /Remove from collection/i,
    })
    expect(
      within(dialog).getByText('Bulbasaur', { selector: 'p' }),
    ).toBeInTheDocument()
    expect(
      within(dialog).getByText(/public catalog is not affected/i),
    ).toBeInTheDocument()

    await user.click(within(dialog).getByRole('button', { name: /^Cancel$/i }))
    await waitFor(() => {
      expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
    })
    expect(deleteMock).not.toHaveBeenCalled()
    expect(screen.getByRole('article', { name: /bulbasaur/i })).toBeInTheDocument()
  })

  it('confirm deletes and navigates to /collection', async () => {
    seedSession()
    const user = userEvent.setup()
    deleteMock.mockResolvedValue(undefined)
    const router = renderCollection()

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /^Remove$/i })).toBeInTheDocument()
    })
    await user.click(screen.getByRole('button', { name: /^Remove$/i }))
    const dialog = await screen.findByRole('alertdialog')
    await user.click(
      within(dialog).getByRole('button', { name: /^Remove from collection$/i }),
    )

    await waitFor(() => {
      expect(deleteMock).toHaveBeenCalledWith(expect.anything(), 1)
    })
    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/collection')
    })
  })

  it('404 on delete is treated as already removed', async () => {
    seedSession()
    const user = userEvent.setup()
    deleteMock.mockRejectedValue(
      new ApiError({
        status: 404,
        type: 'urn:problem-type:not-found',
        title: 'Not Found',
        detail: 'gone',
      }),
    )
    const router = renderCollection()

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /^Remove$/i })).toBeInTheDocument()
    })
    await user.click(screen.getByRole('button', { name: /^Remove$/i }))
    const dialog = await screen.findByRole('alertdialog')
    await user.click(
      within(dialog).getByRole('button', { name: /^Remove from collection$/i }),
    )

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/collection')
    })
  })
})
