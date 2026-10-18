import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import type { AuthResponse } from '@/api/types'
import { AuthProvider } from '@/features/auth/AuthProvider'
import { useAuth } from '@/features/auth/useAuth'
import { SESSION_KEY, save } from '@/lib/session'

const authResponse: AuthResponse = {
  accessToken: 'live-token',
  tokenType: 'Bearer',
  expiresIn: 3600,
  user: { id: 1, name: 'Ash', email: 'ash@example.com' },
}

function AuthProbe() {
  const { user, isAuthenticated, getToken, signIn, signOut } = useAuth()
  return (
    <div>
      <span data-testid="status">
        {isAuthenticated ? 'signed-in' : 'signed-out'}
      </span>
      <span data-testid="user">{user?.name ?? ''}</span>
      <span data-testid="token">{getToken() ?? ''}</span>
      <button type="button" onClick={() => signIn(authResponse)}>
        Sign in
      </button>
      <button type="button" onClick={() => signOut()}>
        Sign out
      </button>
    </div>
  )
}

describe('AuthProvider', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('defaults to signed out', () => {
    render(
      <AuthProvider>
        <AuthProbe />
      </AuthProvider>,
    )

    expect(screen.getByTestId('status')).toHaveTextContent('signed-out')
    expect(screen.getByTestId('user')).toHaveTextContent('')
    expect(screen.getByTestId('token')).toHaveTextContent('')
  })

  it('signIn updates consumers', async () => {
    const user = userEvent.setup()
    render(
      <AuthProvider>
        <AuthProbe />
      </AuthProvider>,
    )

    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(screen.getByTestId('status')).toHaveTextContent('signed-in')
    expect(screen.getByTestId('user')).toHaveTextContent('Ash')
    expect(screen.getByTestId('token')).toHaveTextContent('live-token')
  })

  it('signOut clears session locally', async () => {
    const user = userEvent.setup()
    save(authResponse, Date.now())

    render(
      <AuthProvider>
        <AuthProbe />
      </AuthProvider>,
    )

    expect(screen.getByTestId('status')).toHaveTextContent('signed-in')

    await user.click(screen.getByRole('button', { name: 'Sign out' }))

    expect(screen.getByTestId('status')).toHaveTextContent('signed-out')
    expect(screen.getByTestId('token')).toHaveTextContent('')
    expect(localStorage.getItem(SESSION_KEY)).toBeNull()
  })

  it('expired session on mount is signed out', () => {
    save(authResponse, Date.now() - 7200 * 1000)

    render(
      <AuthProvider>
        <AuthProbe />
      </AuthProvider>,
    )

    expect(screen.getByTestId('status')).toHaveTextContent('signed-out')
    expect(localStorage.getItem(SESSION_KEY)).toBeNull()
  })

  it('syncs when another tab updates storage', () => {
    render(
      <AuthProvider>
        <AuthProbe />
      </AuthProvider>,
    )

    expect(screen.getByTestId('status')).toHaveTextContent('signed-out')

    const stored = {
      token: 'tab-token',
      expiresAt: Date.now() + 60_000,
      user: { id: 9, name: 'Brock', email: 'brock@example.com' },
    }

    act(() => {
      localStorage.setItem(SESSION_KEY, JSON.stringify(stored))
      window.dispatchEvent(
        new StorageEvent('storage', {
          key: SESSION_KEY,
          newValue: JSON.stringify(stored),
          storageArea: localStorage,
        }),
      )
    })

    expect(screen.getByTestId('status')).toHaveTextContent('signed-in')
    expect(screen.getByTestId('user')).toHaveTextContent('Brock')
    expect(screen.getByTestId('token')).toHaveTextContent('tab-token')
  })

  it('signs out exactly at expiry via timer', () => {
    vi.useFakeTimers()
    const now = new Date('2026-10-18T00:00:00.000Z')
    vi.setSystemTime(now)

    save(
      {
        ...authResponse,
        expiresIn: 60,
      },
      now.getTime(),
    )

    render(
      <AuthProvider>
        <AuthProbe />
      </AuthProvider>,
    )

    expect(screen.getByTestId('status')).toHaveTextContent('signed-in')

    act(() => {
      vi.advanceTimersByTime(59_999)
    })
    expect(screen.getByTestId('status')).toHaveTextContent('signed-in')

    act(() => {
      vi.advanceTimersByTime(1)
    })
    expect(screen.getByTestId('status')).toHaveTextContent('signed-out')
    expect(screen.getByTestId('token')).toHaveTextContent('')
    expect(localStorage.getItem(SESSION_KEY)).toBeNull()
  })
})
