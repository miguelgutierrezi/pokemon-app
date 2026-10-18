import { createContext } from 'react'

export type AnnouncerContextValue = {
  announce: (message: string) => void
}

export const AnnouncerContext = createContext<AnnouncerContextValue | null>(
  null,
)
