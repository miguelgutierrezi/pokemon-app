import { render, screen, waitFor, within } from '@testing-library/react'
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

import { login } from '@/api/auth'

const loginMock = vi.mocked(login)

function authResponse(
  overrides: Partial<AuthResponse> = {},
): AuthResponse {
  return {
    accessToken: 'token-abc',
    tokenType: 'Bearer',
    expiresIn: 3600,
    user: {
      id: 1,
      name: 'Demo Trainer',
      email: 'demo@pokefolio.dev',
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

function renderLogin(initialEntry = '/login') {
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

describe('LoginPage', () => {
  beforeEach(() => {
    loginMock.mockReset()
    localStorage.clear()
  })

  it('renders labelled email/password fields, visibility toggle, and demo credential hint', () => {
    renderLogin('/login')

    expect(screen.getByLabelText(/email/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/^password$/i)).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: /show password|hide password/i }),
    ).toBeInTheDocument()
    expect(screen.getByText(/demo credential/i)).toBeInTheDocument()
    expect(screen.getByText(/demo@pokefolio\.dev/i)).toBeInTheDocument()
    expect(screen.getByText(/Demo-Passw0rd!/)).toBeInTheDocument()
    expect(
      screen.getByText(
        /Sign in to add Pokémon to the shared collection and edit their details/i,
      ),
    ).toBeInTheDocument()
    expect(screen.queryByText(/saved to your account/i)).not.toBeInTheDocument()
  })

  it('shows Your session expired when reason=expired', () => {
    renderLogin('/login?reason=expired')
    expect(screen.getByText('Your session expired')).toBeInTheDocument()
  })

  it('submits login once, disables the button while pending, then signs in and redirects', async () => {
    let resolveLogin!: (value: AuthResponse) => void
    loginMock.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveLogin = resolve
        }),
    )

    const router = renderLogin('/login?redirect=/collection')
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/email/i), 'demo@pokefolio.dev')
    await user.type(screen.getByLabelText(/^password$/i), 'Demo-Passw0rd!')
    await user.click(screen.getByRole('button', { name: /^sign in$/i }))

    expect(loginMock).toHaveBeenCalledTimes(1)
    expect(loginMock).toHaveBeenCalledWith(expect.anything(), {
      email: 'demo@pokefolio.dev',
      password: 'Demo-Passw0rd!',
    })

    const busyButton = screen.getByRole('button', {
      name: /signing in|sign in/i,
    })
    expect(busyButton).toBeDisabled()

    resolveLogin(authResponse())
    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/collection')
    })
    expect(localStorage.getItem(SESSION_KEY)).toContain('token-abc')
  })

  it('sanitises open-redirect values to /', async () => {
    loginMock.mockResolvedValue(authResponse())
    const router = renderLogin('/login?redirect=https://evil.example')
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/email/i), 'demo@pokefolio.dev')
    await user.type(screen.getByLabelText(/^password$/i), 'Demo-Passw0rd!')
    await user.click(screen.getByRole('button', { name: /^sign in$/i }))

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/')
    })
  })

  it('shows Invalid email or password on 401 and keeps field values', async () => {
    loginMock.mockRejectedValue(
      new ApiError({
        status: 401,
        type: 'urn:problem-type:unauthorized',
        title: 'Unauthorized',
        detail: 'Bad credentials',
      }),
    )
    renderLogin('/login')
    const user = userEvent.setup()

    await user.type(screen.getByLabelText(/email/i), 'demo@pokefolio.dev')
    await user.type(screen.getByLabelText(/^password$/i), 'wrong')
    await user.click(screen.getByRole('button', { name: /^sign in$/i }))

    const alert = await screen.findByRole('alert')
    expect(within(alert).getByText('Invalid email or password')).toBeInTheDocument()
    expect(screen.getByLabelText(/email/i)).toHaveValue('demo@pokefolio.dev')
    expect(screen.getByLabelText(/^password$/i)).toHaveValue('wrong')
  })

  it('redirects immediately when already signed in', async () => {
    seedSession()
    const router = renderLogin('/login?redirect=/collection')

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/collection')
    })
    expect(screen.queryByLabelText(/email/i)).not.toBeInTheDocument()
  })

  it('links to register preserving redirect and browse without account goes home', () => {
    renderLogin('/login?redirect=/collection')

    expect(screen.getByRole('link', { name: /create account/i })).toHaveAttribute(
      'href',
      '/register?redirect=%2Fcollection',
    )
    expect(
      screen.getByRole('link', { name: /browse without an account/i }),
    ).toHaveAttribute('href', '/')
  })

  it('toggles password visibility with an accessible name', async () => {
    renderLogin('/login')
    const user = userEvent.setup()
    const password = screen.getByLabelText(/^password$/i)
    expect(password).toHaveAttribute('type', 'password')

    await user.click(
      screen.getByRole('button', { name: /show password/i }),
    )
    expect(password).toHaveAttribute('type', 'text')
    await user.click(
      screen.getByRole('button', { name: /hide password/i }),
    )
    expect(password).toHaveAttribute('type', 'password')
  })
})
