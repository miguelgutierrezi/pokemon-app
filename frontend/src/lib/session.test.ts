import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import type { AuthResponse } from '@/api/types'
import {
  SESSION_KEY,
  clear,
  load,
  save,
  subscribe,
} from '@/lib/session'

const authResponse: AuthResponse = {
  accessToken: 'tok-abc',
  tokenType: 'Bearer',
  expiresIn: 3600,
  user: { id: 1, name: 'Ash', email: 'ash@example.com' },
}

describe('session store', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  afterEach(() => {
    localStorage.clear()
  })

  it('save persists token, expiresAt, and user under pokefolio.session', () => {
    const now = 1_700_000_000_000
    save(authResponse, now)

    const raw = localStorage.getItem(SESSION_KEY)
    expect(raw).not.toBeNull()
    expect(JSON.parse(raw!)).toEqual({
      token: 'tok-abc',
      expiresAt: now + 3600 * 1000,
      user: { id: 1, name: 'Ash', email: 'ash@example.com' },
    })
    expect(SESSION_KEY).toBe('pokefolio.session')
  })

  it('load returns the snapshot when valid and not expired', () => {
    const now = 1_700_000_000_000
    save(authResponse, now)

    expect(load(now + 1000)).toEqual({
      token: 'tok-abc',
      expiresAt: now + 3600 * 1000,
      user: { id: 1, name: 'Ash', email: 'ash@example.com' },
    })
  })

  it('load returns null and clears when missing', () => {
    expect(load(Date.now())).toBeNull()
    expect(localStorage.getItem(SESSION_KEY)).toBeNull()
  })

  it('load returns null and clears corrupted JSON', () => {
    localStorage.setItem(SESSION_KEY, '{not-json')
    expect(load(Date.now())).toBeNull()
    expect(localStorage.getItem(SESSION_KEY)).toBeNull()
  })

  it('load returns null and clears wrong shape', () => {
    localStorage.setItem(
      SESSION_KEY,
      JSON.stringify({ accessToken: 'legacy', expiresAt: 99, user: {} }),
    )
    expect(load(Date.now())).toBeNull()
    expect(localStorage.getItem(SESSION_KEY)).toBeNull()
  })

  it('load returns null and clears when expired', () => {
    const now = 1_700_000_000_000
    save(authResponse, now)
    expect(load(now + 3600 * 1000)).toBeNull()
    expect(localStorage.getItem(SESSION_KEY)).toBeNull()
  })

  it('clear removes the session', () => {
    save(authResponse, 1_700_000_000_000)
    clear()
    expect(localStorage.getItem(SESSION_KEY)).toBeNull()
    expect(load(1_700_000_000_000)).toBeNull()
  })

  it('treats localStorage failures as signed out', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('blocked')
    })
    expect(load(Date.now())).toBeNull()
  })

  it('subscribe notifies on storage events for the session key', () => {
    const listener = vi.fn()
    const unsubscribe = subscribe(listener)

    const stored = {
      token: 'from-other-tab',
      expiresAt: Date.now() + 60_000,
      user: { id: 2, name: 'Misty', email: 'misty@example.com' },
    }
    localStorage.setItem(SESSION_KEY, JSON.stringify(stored))
    window.dispatchEvent(
      new StorageEvent('storage', {
        key: SESSION_KEY,
        newValue: JSON.stringify(stored),
        storageArea: localStorage,
      }),
    )

    expect(listener).toHaveBeenCalledWith(stored)

    localStorage.removeItem(SESSION_KEY)
    window.dispatchEvent(
      new StorageEvent('storage', {
        key: SESSION_KEY,
        newValue: null,
        storageArea: localStorage,
      }),
    )
    expect(listener).toHaveBeenCalledWith(null)

    unsubscribe()
    window.dispatchEvent(
      new StorageEvent('storage', {
        key: SESSION_KEY,
        newValue: null,
        storageArea: localStorage,
      }),
    )
    expect(listener).toHaveBeenCalledTimes(2)
  })

  it('subscribe ignores storage events for other keys', () => {
    const listener = vi.fn()
    const unsubscribe = subscribe(listener)

    window.dispatchEvent(
      new StorageEvent('storage', {
        key: 'other',
        newValue: 'x',
        storageArea: localStorage,
      }),
    )
    expect(listener).not.toHaveBeenCalled()
    unsubscribe()
  })

  it('subscribe notifies null when storage key is null (clear)', () => {
    const listener = vi.fn()
    const unsubscribe = subscribe(listener)

    save(authResponse, Date.now())
    window.dispatchEvent(
      new StorageEvent('storage', {
        key: null,
        newValue: null,
        storageArea: localStorage,
      }),
    )

    expect(listener).toHaveBeenCalledWith(null)
    unsubscribe()
  })
})
