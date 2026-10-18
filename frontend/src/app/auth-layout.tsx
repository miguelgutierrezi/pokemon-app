import { useEffect, useRef } from 'react'
import { Outlet, useLocation } from 'react-router'

import { AuthBrandPanel } from '@/features/auth/AuthBrandPanel'

/**
 * Split-screen chrome for /login and /register (Figma 5-233 / 5-921).
 */
export function AuthLayout() {
  const location = useLocation()
  const mainRef = useRef<HTMLElement>(null)

  useEffect(() => {
    const heading = mainRef.current?.querySelector('h1')
    heading?.focus()
  }, [location.pathname, location.search])

  return (
    <div className="flex min-h-screen flex-col bg-card text-foreground lg:flex-row">
      <AuthBrandPanel />
      <main
        ref={mainRef}
        className="flex flex-1 flex-col items-center px-6 py-8 md:px-10 lg:justify-center lg:px-16 lg:py-10"
      >
        <div className="mb-6 flex w-full max-w-md justify-end lg:absolute lg:top-14 lg:right-16 lg:mb-0 lg:max-w-none">
          <div className="flex items-center gap-2">
            <span className="size-1.5 rounded-[2px] bg-primary" aria-hidden />
            <p className="font-mono text-caption tracking-wide text-muted-foreground uppercase">
              Pokédex /{' '}
              {location.pathname.startsWith('/register') ? 'Sign up' : 'Login'}
            </p>
          </div>
        </div>
        <Outlet />
      </main>
    </div>
  )
}
