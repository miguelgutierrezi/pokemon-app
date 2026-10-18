import { render, screen, waitFor } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

vi.mock('@/api/pokemon', () => ({
  listPokemon: vi.fn().mockResolvedValue({
    content: [],
    page: 1,
    size: 9,
    totalElements: 0,
    totalPages: 0,
  }),
}))

import App from '@/App'

describe('App', () => {
  it('boots the catalog route under the app shell', async () => {
    render(<App />)

    await waitFor(() => {
      expect(
        screen.getByRole('heading', { level: 1, name: 'The Pokémon catalog' }),
      ).toBeInTheDocument()
    })
  })
})
