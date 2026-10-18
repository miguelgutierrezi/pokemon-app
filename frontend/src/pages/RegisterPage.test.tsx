import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { ApiError } from '@/api/api-error'
import type { AuthResponse } from '@/api/types'
import { AppProviders } from '@/app/providers'
import { routerConfig } from '@/app/routes'
import { SESSION_KEY } from '@/lib/session'

vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  register: vi.fn(),
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

vi.mock('@/api/collection', () => ({
  getLocalPokemonById: vi.fn(),
}))

import { register } from '@/api/auth'

const registerMock = vi.mocked(register)

function authResponse(
  overrides: Partial<AuthResponse> = {},
): AuthResponse {
  return {
    accessToken: 'token-reg',
    tokenType: 'Bearer',
    expiresIn: 3600,
    user: {
      id: 2,
      name: 'New Trainer',
      email: 'new@pokefolio.dev',
    },
    ...overrides,
  }
}

function seedSession() {
  localStorage.setItem(
    SESSION_KEY,
    JSON.stringify({
      token: 'existing',
      expiresAt: Date.now() + 60_000,
      user: { id: 1, name: 'Demo', email: 'demo@pokefolio.dev' },
    }),
  )
}

function renderRegister(initialEntry = '/register') {
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

async function fillValidForm(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText(/^name$/i), 'New Trainer')
  await user.type(screen.getByLabelText(/email/i), 'new@pokefolio.dev')
  await user.type(screen.getByLabelText(/^password$/i), 'Demo-Passw0rd!')
}

describe('RegisterPage', () => {
  beforeEach(() => {
    registerMock.mockReset()
    localStorage.clear()
  })

  it('renders labelled fields and shared-collection copy', () => {
    renderRegister('/register')

    expect(screen.getByLabelText(/^name$/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/^password$/i)).toBeInTheDocument()
    expect(
      screen.getByText(
        /Create an account to add Pokémon to the shared collection and edit their details/i,
      ),
    ).toBeInTheDocument()
    expect(screen.queryByText(/saved to your account/i)).not.toBeInTheDocument()
  })

  it('validates client-side and does not call the API when invalid', async () => {
    renderRegister('/register')
    const user = userEvent.setup()

    await user.click(screen.getByRole('button', { name: /create account/i }))

    expect(registerMock).not.toHaveBeenCalled()
    const name = screen.getByLabelText(/^name$/i)
    expect(name).toHaveAttribute('aria-invalid', 'true')
    expect(name).toHaveAttribute('aria-describedby')
    expect(document.activeElement).toBe(name)
  })

  it('rejects passwords shorter than 8 characters', async () => {
    renderRegister('/register')
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/^name$/i), 'Trainer')
    await user.type(screen.getByLabelText(/email/i), 't@pokefolio.dev')
    await user.type(screen.getByLabelText(/^password$/i), 'short')
    await user.click(screen.getByRole('button', { name: /create account/i }))

    expect(registerMock).not.toHaveBeenCalled()
    const password = screen.getByLabelText(/^password$/i)
    expect(password).toHaveAttribute('aria-invalid', 'true')
  })

  it('rejects a whitespace-only password with the same password message', async () => {
    renderRegister('/register')
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/^name$/i), 'Trainer')
    await user.type(screen.getByLabelText(/email/i), 't@pokefolio.dev')
    await user.type(screen.getByLabelText(/^password$/i), '        ')
    await user.click(screen.getByRole('button', { name: /create account/i }))

    expect(registerMock).not.toHaveBeenCalled()
    expect(screen.getByLabelText(/^password$/i)).toHaveAttribute(
      'aria-invalid',
      'true',
    )
    expect(
      screen.getByText(
        'Password must be at least 8 characters and at most 72 UTF-8 bytes.',
      ),
    ).toBeInTheDocument()
  })

  it('rejects passwords longer than 72 UTF-8 bytes', async () => {
    renderRegister('/register')
    const user = userEvent.setup()
    // 73 ASCII bytes
    const longPassword = 'a'.repeat(73)

    await user.type(screen.getByLabelText(/^name$/i), 'Trainer')
    await user.type(screen.getByLabelText(/email/i), 't@pokefolio.dev')
    await user.type(screen.getByLabelText(/^password$/i), longPassword)
    await user.click(screen.getByRole('button', { name: /create account/i }))

    expect(registerMock).not.toHaveBeenCalled()
    expect(screen.getByLabelText(/^password$/i)).toHaveAttribute(
      'aria-invalid',
      'true',
    )
  })

  it('registers successfully, signs in, and redirects', async () => {
    registerMock.mockResolvedValue(authResponse())
    const router = renderRegister('/register?redirect=/collection')
    const user = userEvent.setup()

    await fillValidForm(user)
    await user.click(screen.getByRole('button', { name: /create account/i }))

    await waitFor(() => {
      expect(registerMock).toHaveBeenCalledWith(expect.anything(), {
        name: 'New Trainer',
        email: 'new@pokefolio.dev',
        password: 'Demo-Passw0rd!',
      })
    })
    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/collection')
    })
    expect(localStorage.getItem(SESSION_KEY)).toContain('token-reg')
  })

  it('maps server 400 field errors onto the matching field', async () => {
    registerMock.mockRejectedValue(
      new ApiError({
        status: 400,
        type: 'urn:problem-type:bad-request',
        title: 'Bad Request',
        detail: "Invalid value for property 'email'",
        field: 'email',
      }),
    )
    renderRegister('/register')
    const user = userEvent.setup()

    await fillValidForm(user)
    await user.click(screen.getByRole('button', { name: /create account/i }))

    await waitFor(() => {
      expect(screen.getByLabelText(/email/i)).toHaveAttribute(
        'aria-invalid',
        'true',
      )
    })
  })

  it('shows account-exists error on the email field for 409', async () => {
    registerMock.mockRejectedValue(
      new ApiError({
        status: 409,
        type: 'urn:problem-type:conflict',
        title: 'Conflict',
        detail: 'already exists',
      }),
    )
    renderRegister('/register')
    const user = userEvent.setup()

    await fillValidForm(user)
    await user.click(screen.getByRole('button', { name: /create account/i }))

    await waitFor(() => {
      expect(
        screen.getByText('An account with this email already exists'),
      ).toBeInTheDocument()
    })
    expect(screen.getByLabelText(/email/i)).toHaveAttribute(
      'aria-invalid',
      'true',
    )
  })

  it('redirects immediately when already signed in', async () => {
    seedSession()
    const router = renderRegister('/register?redirect=/collection')

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/collection')
    })
    expect(screen.queryByLabelText(/^name$/i)).not.toBeInTheDocument()
  })

  it('links to login preserving redirect', () => {
    renderRegister('/register?redirect=/collection')
    expect(screen.getByRole('link', { name: /^sign in$/i })).toHaveAttribute(
      'href',
      '/login?redirect=%2Fcollection',
    )
  })
})
