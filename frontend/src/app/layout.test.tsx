import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { AuthResponse } from '@/api/types'
import { AppProviders } from '@/app/providers'
import { routerConfig } from '@/app/routes'
import { SESSION_KEY, save } from '@/lib/session'

vi.mock('@/api/pokemon', () => ({
  listPokemon: vi.fn().mockResolvedValue({
    content: [],
    page: 1,
    size: 9,
    totalElements: 0,
    totalPages: 0,
  }),
}))

const authResponse: AuthResponse = {
  accessToken: 'layout-token',
  tokenType: 'Bearer',
  expiresIn: 3600,
  user: { id: 1, name: 'Ash', email: 'ash@example.com' },
}

function renderAt(path: string) {
  const router = createMemoryRouter(routerConfig, {
    initialEntries: [path],
  })
  render(
    <AppProviders>
      <RouterProvider router={router} />
    </AppProviders>,
  )
  return router
}

describe('AppLayout', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('exposes header, nav, main, footer landmarks and brand', () => {
    renderAt('/')

    expect(screen.getByRole('banner')).toBeInTheDocument()
    expect(
      screen.getByRole('navigation', { name: /primary/i }),
    ).toBeInTheDocument()
    expect(screen.getByRole('main')).toBeInTheDocument()
    expect(screen.getByRole('contentinfo')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /pokéfolio/i })).toBeInTheDocument()
    expect(
      screen.getByRole('link', { name: 'Pokémon catalog' }),
    ).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Collection' })).toBeInTheDocument()
    expect(document.querySelector('[aria-live="polite"]')).not.toBeNull()
  })

  it('focuses the page h1 after navigation', async () => {
    const router = renderAt('/')
    const catalogHeading = screen.getByRole('heading', {
      level: 1,
      name: 'The Pokémon catalog',
    })
    expect(catalogHeading).toHaveFocus()

    await act(async () => {
      await router.navigate('/collection')
    })

    expect(
      screen.getByRole('heading', { level: 1, name: 'Collection' }),
    ).toHaveFocus()
  })

  it('signed-out chrome shows Sign in and Create account', () => {
    renderAt('/')

    expect(screen.getByRole('link', { name: 'Sign in' })).toHaveAttribute(
      'href',
      '/login',
    )
    expect(
      screen.getByRole('link', { name: 'Create account' }),
    ).toHaveAttribute('href', '/register')
  })

  it('Logout clears session client-side without network and stays on page', async () => {
    const user = userEvent.setup()
    save(authResponse, Date.now())
    const fetchSpy = vi.spyOn(globalThis, 'fetch')

    renderAt('/collection')

    expect(screen.getByRole('button', { name: 'Logout' })).toBeInTheDocument()
    expect(
      screen.getByRole('heading', { level: 1, name: 'Collection' }),
    ).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Logout' }))

    expect(localStorage.getItem(SESSION_KEY)).toBeNull()
    expect(fetchSpy).not.toHaveBeenCalled()
    expect(
      screen.getByRole('heading', { level: 1, name: 'Collection' }),
    ).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Sign in' })).toBeInTheDocument()

    fetchSpy.mockRestore()
  })
})

describe('AuthLayout', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('renders login without app nav or footer', () => {
    renderAt('/login')

    expect(screen.getByRole('heading', { level: 1, name: 'Sign in' })).toBeInTheDocument()
    expect(screen.getByRole('main')).toBeInTheDocument()
    expect(screen.queryByRole('banner')).not.toBeInTheDocument()
    expect(screen.queryByRole('contentinfo')).not.toBeInTheDocument()
    expect(
      screen.queryByRole('navigation', { name: /primary/i }),
    ).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1, name: 'Sign in' })).toHaveFocus()
  })

  it('renders register without app nav or footer', () => {
    renderAt('/register')

    expect(
      screen.getByRole('heading', { level: 1, name: 'Create account' }),
    ).toBeInTheDocument()
    expect(screen.queryByRole('banner')).not.toBeInTheDocument()
    expect(screen.queryByRole('contentinfo')).not.toBeInTheDocument()
  })
})
