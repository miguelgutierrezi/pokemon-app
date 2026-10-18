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

import {
  deleteLocalPokemon,
  getLocalPokemonById,
  updateLocalPokemon,
} from '@/api/collection'

const getLocalMock = vi.mocked(getLocalPokemonById)
const updateMock = vi.mocked(updateLocalPokemon)
const deleteMock = vi.mocked(deleteLocalPokemon)

function savedPokemon(overrides: Partial<LocalPokemon> = {}): LocalPokemon {
  return {
    nationalNumber: 1,
    name: 'Bulbasaur',
    types: ['grass', 'poison'],
    heightM: 0.7,
    weightKg: 6.9,
    category: 'Seed Pokémon',
    generation: 'generation-i',
    imageUrl: 'https://example.com/1.png',
    syncedAt: '2026-01-01T00:00:00Z',
    localizedName: 'Bulbizarre',
    location: 'Pallet Town',
    tags: ['first-partner', 'forest'],
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

function renderEdit(initialEntry = '/collection/1') {
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

describe('CollectionEditPage', () => {
  beforeEach(() => {
    getLocalMock.mockReset()
    updateMock.mockReset()
    deleteMock.mockReset()
    localStorage.clear()
    getLocalMock.mockResolvedValue(savedPokemon())
  })

  it('loads GET /api/v1/collection/:id and fills the form with version', async () => {
    seedSession()
    renderEdit('/collection/1')

    await waitFor(() => {
      expect(getLocalMock).toHaveBeenCalledWith(1, expect.any(AbortSignal))
    })
    await waitFor(() => {
      expect(screen.getByLabelText(/Localized name/i)).toHaveValue('Bulbizarre')
    })
    expect(screen.getByLabelText(/^Location$/i)).toHaveValue('Pallet Town')
    expect(screen.getByText('first-partner')).toBeInTheDocument()
    expect(screen.getByText('forest')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: /Edit Pokémon/i })).toBeInTheDocument()
  })

  it('invalid id shows not-found without calling the API', async () => {
    renderEdit('/collection/abc')

    await waitFor(() => {
      expect(screen.getByText(/Not in the collection/i)).toBeInTheDocument()
    })
    expect(getLocalMock).not.toHaveBeenCalled()
  })

  it('404 shows Not in the collection with catalog detail and collection links', async () => {
    getLocalMock.mockRejectedValue(
      new ApiError({
        status: 404,
        type: 'urn:problem-type:not-found',
        title: 'Not Found',
        detail: 'Local Pokémon not found',
      }),
    )

    renderEdit('/collection/25')

    await waitFor(() => {
      expect(screen.getByText(/Not in the collection/i)).toBeInTheDocument()
    })
    expect(
      screen.getByRole('link', { name: 'View in catalog' }),
    ).toHaveAttribute('href', '/pokemon/25')
    expect(
      screen.getByRole('link', { name: 'Back to collection' }),
    ).toHaveAttribute('href', '/collection')
  })

  it('signed out: form is read-only with Sign in to edit CTA', async () => {
    renderEdit('/collection/1')

    await waitFor(() => {
      expect(screen.getByLabelText(/Localized name/i)).toBeDisabled()
    })
    expect(screen.getByLabelText(/^Location$/i)).toBeDisabled()
    expect(
      screen.getByRole('link', { name: /Sign in to edit/i }),
    ).toHaveAttribute('href', '/login?redirect=%2Fcollection%2F1')
    expect(screen.queryByRole('button', { name: /Save changes/i })).not.toBeInTheDocument()
  })

  it('client validation blocks save and focuses first invalid field', async () => {
    seedSession()
    const user = userEvent.setup()
    renderEdit('/collection/1')

    await waitFor(() => {
      expect(screen.getByLabelText(/Localized name/i)).toBeInTheDocument()
    })

    await user.clear(screen.getByLabelText(/Localized name/i))
    await user.type(
      screen.getByLabelText(/Localized name/i),
      'x'.repeat(101),
    )
    await user.click(screen.getByRole('button', { name: /Save changes/i }))

    expect(updateMock).not.toHaveBeenCalled()
    expect(screen.getByLabelText(/Localized name/i)).toHaveAttribute(
      'aria-invalid',
      'true',
    )
    expect(screen.getByLabelText(/Localized name/i)).toHaveFocus()
  })

  it('saves with full PUT body including version, no confirmation modal, then navigates', async () => {
    seedSession()
    const user = userEvent.setup()
    updateMock.mockResolvedValue(
      savedPokemon({ localizedName: 'Sparky', version: 3 }),
    )

    const router = renderEdit('/collection/1')
    await waitFor(() => {
      expect(screen.getByLabelText(/Localized name/i)).toHaveValue('Bulbizarre')
    })

    await user.clear(screen.getByLabelText(/Localized name/i))
    await user.type(screen.getByLabelText(/Localized name/i), 'Sparky')
    await user.click(screen.getByRole('button', { name: /Save changes/i }))

    await waitFor(() => {
      expect(updateMock).toHaveBeenCalled()
    })
    expect(updateMock).toHaveBeenCalledWith(
      expect.anything(),
      1,
      {
        localizedName: 'Sparky',
        location: 'Pallet Town',
        tags: ['first-partner', 'forest'],
        version: 2,
      },
    )
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/collection')
    })
  })

  it('sends null for empty localizedName and location', async () => {
    seedSession()
    const user = userEvent.setup()
    updateMock.mockResolvedValue(savedPokemon({ localizedName: null, location: null }))

    renderEdit('/collection/1')
    await waitFor(() => {
      expect(screen.getByLabelText(/Localized name/i)).toBeInTheDocument()
    })

    await user.clear(screen.getByLabelText(/Localized name/i))
    await user.clear(screen.getByLabelText(/^Location$/i))
    await user.click(screen.getByRole('button', { name: /Save changes/i }))

    await waitFor(() => {
      expect(updateMock).toHaveBeenCalledWith(
        expect.anything(),
        1,
        expect.objectContaining({
          localizedName: null,
          location: null,
          tags: ['first-partner', 'forest'],
          version: 2,
        }),
      )
    })
  })

  it('400 with field shows inline error on that field', async () => {
    seedSession()
    const user = userEvent.setup()
    updateMock.mockRejectedValue(
      new ApiError({
        status: 400,
        type: 'urn:problem-type:bad-request',
        title: 'Bad Request',
        detail: "property 'tags' must be a list of text labels",
        field: 'tags',
      }),
    )

    renderEdit('/collection/1')
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Save changes/i })).toBeInTheDocument()
    })
    await user.click(screen.getByRole('button', { name: /Save changes/i }))

    await waitFor(() => {
      expect(
        screen.getByText("property 'tags' must be a list of text labels"),
      ).toBeInTheDocument()
    })
    expect(screen.getByRole('textbox', { name: /^Tags$/i })).toHaveAttribute(
      'aria-invalid',
      'true',
    )
  })

  it('409 stale-version shows Modified elsewhere — reload and Reload resets the form', async () => {
    seedSession()
    const user = userEvent.setup()
    updateMock.mockRejectedValue(
      new ApiError({
        status: 409,
        type: 'urn:problem-type:stale-version',
        title: 'Conflict',
        detail: 'Version conflict',
      }),
    )

    renderEdit('/collection/1')
    await waitFor(() => {
      expect(screen.getByLabelText(/Localized name/i)).toHaveValue('Bulbizarre')
    })

    await user.clear(screen.getByLabelText(/Localized name/i))
    await user.type(screen.getByLabelText(/Localized name/i), 'Draft name')
    await user.click(screen.getByRole('button', { name: /Save changes/i }))

    await waitFor(() => {
      expect(screen.getByText(/Modified elsewhere — reload/i)).toBeInTheDocument()
    })

    getLocalMock.mockResolvedValue(
      savedPokemon({ localizedName: 'Server wins', version: 5 }),
    )
    await user.click(screen.getByRole('button', { name: /^Reload$/i }))

    await waitFor(() => {
      expect(screen.getByLabelText(/Localized name/i)).toHaveValue('Server wins')
    })
    expect(screen.queryByText('Draft name')).not.toBeInTheDocument()
  })

  it('404 on save shows Not in the collection', async () => {
    seedSession()
    const user = userEvent.setup()
    updateMock.mockRejectedValue(
      new ApiError({
        status: 404,
        type: 'urn:problem-type:not-found',
        title: 'Not Found',
        detail: 'gone',
      }),
    )

    renderEdit('/collection/1')
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Save changes/i })).toBeInTheDocument()
    })
    await user.click(screen.getByRole('button', { name: /Save changes/i }))

    await waitFor(() => {
      expect(screen.getByText(/Not in the collection/i)).toBeInTheDocument()
    })
  })

  it('401 on save does not show success or navigate away', async () => {
    seedSession()
    const user = userEvent.setup()
    updateMock.mockRejectedValue(
      new ApiError({
        status: 401,
        type: 'urn:problem-type:unauthorized',
        title: 'Unauthorized',
        detail: 'expired',
      }),
    )

    const router = renderEdit('/collection/1')
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Save changes/i })).toBeInTheDocument()
    })
    await user.click(screen.getByRole('button', { name: /Save changes/i }))

    await waitFor(() => {
      expect(updateMock).toHaveBeenCalled()
    })
    expect(router.state.location.pathname).toBe('/collection/1')
    expect(screen.queryByText(/updated in the collection/i)).not.toBeInTheDocument()
  })

  it('adds tags via Enter and Add, normalises lowercase, rejects duplicates', async () => {
    seedSession()
    const user = userEvent.setup()
    renderEdit('/collection/1')

    await waitFor(() => {
      expect(screen.getByRole('textbox', { name: /^Tags$/i })).toBeInTheDocument()
    })

    const tagInput = screen.getByRole('textbox', { name: /^Tags$/i })
    await user.type(tagInput, 'Favorites{Enter}')
    expect(screen.getByText('favorites')).toBeInTheDocument()

    await user.type(tagInput, 'FAVORITES')
    await user.click(screen.getByRole('button', { name: /^Add$/i }))
    expect(
      within(screen.getByRole('list', { name: /^Tags$/i })).getAllByText(
        'favorites',
      ),
    ).toHaveLength(1)

    await user.click(
      screen.getByRole('button', { name: /Remove tag first-partner/i }),
    )
    expect(screen.queryByText('first-partner')).not.toBeInTheDocument()
  })

  it('commits pending tag text on Save without Enter', async () => {
    seedSession()
    const user = userEvent.setup()
    getLocalMock.mockResolvedValue(savedPokemon({ tags: [] }))
    updateMock.mockResolvedValue(savedPokemon({ tags: ['starter'] }))

    renderEdit('/collection/1')
    await waitFor(() => {
      expect(screen.getByRole('textbox', { name: /^Tags$/i })).toBeInTheDocument()
    })

    await user.type(screen.getByRole('textbox', { name: /^Tags$/i }), 'Starter')
    await user.click(screen.getByRole('button', { name: /Save changes/i }))

    await waitFor(() => {
      expect(updateMock).toHaveBeenCalledWith(
        expect.anything(),
        1,
        expect.objectContaining({
          tags: ['starter'],
          version: 2,
        }),
      )
    })
  })

  it('splits pasted comma-separated tags and keeps the trailing fragment', async () => {
    seedSession()
    const user = userEvent.setup()
    getLocalMock.mockResolvedValue(savedPokemon({ tags: [] }))

    renderEdit('/collection/1')
    await waitFor(() => {
      expect(screen.getByRole('textbox', { name: /^Tags$/i })).toBeInTheDocument()
    })

    const tagInput = screen.getByRole('textbox', { name: /^Tags$/i })
    await user.click(tagInput)
    await user.paste('fire, water')

    expect(screen.getByText('fire')).toBeInTheDocument()
    expect(screen.queryByText('water')).not.toBeInTheDocument()
    expect(tagInput).toHaveValue(' water')
  })

  it('Remove from collection opens dialog; Cancel does not delete; Confirm deletes and navigates', async () => {
    seedSession()
    const user = userEvent.setup()
    deleteMock.mockResolvedValue(undefined)

    const router = renderEdit('/collection/1')
    await waitFor(() => {
      expect(
        screen.getByRole('button', { name: /Remove from collection/i }),
      ).toBeInTheDocument()
    })

    await user.click(
      screen.getByRole('button', { name: /Remove from collection/i }),
    )
    const dialog = await screen.findByRole('alertdialog', {
      name: /Remove from collection/i,
    })
    expect(
      within(dialog).getByText(/public catalog is not affected/i),
    ).toBeInTheDocument()

    await user.click(within(dialog).getByRole('button', { name: /^Cancel$/i }))
    await waitFor(() => {
      expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
    })
    expect(deleteMock).not.toHaveBeenCalled()

    await user.click(
      screen.getByRole('button', { name: /Remove from collection/i }),
    )
    const confirmDialog = await screen.findByRole('alertdialog')
    await user.click(
      within(confirmDialog).getByRole('button', {
        name: /^Remove from collection$/i,
      }),
    )

    await waitFor(() => {
      expect(deleteMock).toHaveBeenCalledWith(expect.anything(), 1)
    })
    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/collection')
    })
  })

  it('404 on delete navigates to collection as already removed', async () => {
    seedSession()
    const user = userEvent.setup()
    deleteMock.mockRejectedValue(
      new ApiError({
        status: 404,
        type: 'urn:problem-type:not-found',
        title: 'Not Found',
        detail: 'already gone',
      }),
    )

    const router = renderEdit('/collection/1')
    await waitFor(() => {
      expect(
        screen.getByRole('button', { name: /Remove from collection/i }),
      ).toBeInTheDocument()
    })
    await user.click(
      screen.getByRole('button', { name: /Remove from collection/i }),
    )
    const dialog = await screen.findByRole('alertdialog')
    await user.click(
      within(dialog).getByRole('button', { name: /^Remove from collection$/i }),
    )

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/collection')
    })
  })
})
