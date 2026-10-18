import { cleanup } from '@testing-library/react'
import '@testing-library/jest-dom/vitest'
import { afterEach, beforeEach, vi } from 'vitest'

beforeEach(() => {
  vi.spyOn(console, 'error').mockImplementation(() => {})
  vi.spyOn(console, 'warn').mockImplementation(() => {})
})

afterEach(() => {
  // Unmount first so React/Radix cleanup warnings hit the spies still active.
  cleanup()

  const failures: string[] = []
  try {
    for (const method of ['error', 'warn'] as const) {
      const spy = console[method] as unknown as {
        mock: { calls: unknown[][] }
      }
      if (spy.mock.calls.length > 0) {
        const messages = spy.mock.calls
          .map((args) =>
            args
              .map((arg) => (typeof arg === 'string' ? arg : String(arg)))
              .join(' '),
          )
          .join('\n')
        failures.push(`Unexpected console.${method}:\n${messages}`)
      }
    }
  } finally {
    vi.restoreAllMocks()
  }

  if (failures.length > 0) {
    throw new Error(failures.join('\n\n'))
  }
})
