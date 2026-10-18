import { useContext } from 'react'

import { AnnouncerContext } from '@/app/announcer-context'

export function useAnnouncer() {
  const context = useContext(AnnouncerContext)
  if (context == null) {
    throw new Error('useAnnouncer must be used within AnnouncerProvider')
  }
  return context
}
