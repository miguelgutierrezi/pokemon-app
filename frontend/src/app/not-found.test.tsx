import { render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { describe, expect, it } from 'vitest'

import { AppProviders } from '@/app/providers'
import { routerConfig } from '@/app/routes'

describe('NotFound page', () => {
  it('shows accessible heading and link home for unknown routes', () => {
    const router = createMemoryRouter(routerConfig, {
      initialEntries: ['/this-route-does-not-exist'],
    })

    render(
      <AppProviders>
        <RouterProvider router={router} />
      </AppProviders>,
    )

    expect(
      screen.getByRole('heading', { level: 1, name: /page not found/i }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('link', { name: /back to catalog/i }),
    ).toHaveAttribute('href', '/')
  })
})
