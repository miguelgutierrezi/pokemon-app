import { act, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { AnnouncerProvider } from '@/app/announcer'
import { useAnnouncer } from '@/app/useAnnouncer'

function Probe() {
  const { announce } = useAnnouncer()
  return (
    <button type="button" onClick={() => announce('Saved')}>
      Announce
    </button>
  )
}

describe('AnnouncerProvider', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('writes messages into the aria-live polite region', () => {
    render(
      <AnnouncerProvider>
        <Probe />
      </AnnouncerProvider>,
    )

    const live = document.querySelector('[aria-live="polite"]')
    expect(live).not.toBeNull()
    expect(live).toHaveTextContent('')

    act(() => {
      screen.getByRole('button', { name: 'Announce' }).click()
    })
    expect(live).toHaveTextContent('')

    act(() => {
      vi.runAllTimers()
    })
    expect(live).toHaveTextContent('Saved')
  })

  it('clears then sets so repeated messages are re-announced', () => {
    render(
      <AnnouncerProvider>
        <Probe />
      </AnnouncerProvider>,
    )
    const live = document.querySelector('[aria-live="polite"]')!

    act(() => {
      screen.getByRole('button', { name: 'Announce' }).click()
      vi.runAllTimers()
    })
    expect(live).toHaveTextContent('Saved')

    act(() => {
      screen.getByRole('button', { name: 'Announce' }).click()
    })
    expect(live).toHaveTextContent('')

    act(() => {
      vi.runAllTimers()
    })
    expect(live).toHaveTextContent('Saved')
  })
})
