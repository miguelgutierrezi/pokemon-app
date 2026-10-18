import { useCallback, useMemo, useState, type ReactNode } from 'react'

import { AnnouncerContext } from '@/app/announcer-context'

export function AnnouncerProvider({ children }: { children: ReactNode }) {
  const [message, setMessage] = useState('')

  const announce = useCallback((next: string) => {
    // Clear first so identical messages are re-announced by assistive tech.
    setMessage('')
    window.setTimeout(() => {
      setMessage(next)
    }, 0)
  }, [])

  const value = useMemo(() => ({ announce }), [announce])

  return (
    <AnnouncerContext.Provider value={value}>
      {children}
      <div aria-live="polite" className="sr-only">
        {message}
      </div>
    </AnnouncerContext.Provider>
  )
}
