import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import App from '@/App'

describe('App', () => {
  it('boots the catalog route under the app shell', () => {
    render(<App />)

    expect(
      screen.getByRole('heading', { level: 1, name: 'Pokémon catalog' }),
    ).toBeInTheDocument()
  })
})
