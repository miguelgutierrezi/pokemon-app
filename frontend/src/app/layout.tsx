import { useEffect, useRef } from 'react'
import { Link, NavLink, Outlet, useLocation } from 'react-router'

import accountIcon from '@/assets/account-icon.svg'
import pokeballEmblem from '@/assets/pokeball-emblem.svg'
import { Button } from '@/components/ui/button'
import { SessionExpiredDialog } from '@/features/auth/SessionExpiredDialog'
import { useAuth } from '@/features/auth/useAuth'
import { cn } from '@/lib/utils'

function NavItem({
  to,
  label,
  end,
}: {
  to: string
  label: string
  end?: boolean
}) {
  return (
    <NavLink
      to={to}
      end={end}
      className={({ isActive }) =>
        cn(
          'relative flex min-h-11 flex-col justify-center text-body-sm font-semibold whitespace-nowrap md:h-full md:min-h-0',
          isActive
            ? 'text-nav-foreground'
            : 'text-nav-muted hover:text-nav-foreground',
        )
      }
    >
      {({ isActive }) => (
        <>
          <span>{label}</span>
          <span
            aria-hidden
            className={cn(
              'mt-3 block h-[3px] w-full bg-primary md:absolute md:right-0 md:bottom-0 md:left-0 md:mt-0',
              !isActive && 'invisible',
            )}
          />
        </>
      )}
    </NavLink>
  )
}

export function AppLayout() {
  const { isAuthenticated, signOut, user } = useAuth()
  const location = useLocation()
  const mainRef = useRef<HTMLElement>(null)

  useEffect(() => {
    const heading = mainRef.current?.querySelector('h1')
    heading?.focus()
  }, [location.pathname, location.search])

  return (
    <div className="flex min-h-screen flex-col bg-background text-foreground">
      <header className="bg-nav text-nav-foreground">
        <div className="mx-auto flex w-full max-w-6xl flex-col gap-4 px-6 py-6 md:h-22 md:flex-row md:items-center md:gap-16 md:px-14 md:py-0">
          <div className="flex items-center justify-between gap-4 md:contents">
            <Link
              to="/"
              className="flex items-center gap-2.5 text-nav-foreground"
            >
              <img
                src={pokeballEmblem}
                alt=""
                width={32}
                height={32}
                className="size-7 md:size-8"
              />
              <span className="text-title font-semibold md:text-display">
                Pokéfolio
              </span>
            </Link>
            <p className="font-mono text-label tracking-wide text-nav-muted uppercase md:order-last md:ml-auto">
              {isAuthenticated ? 'Signed in' : 'Public access'}
            </p>
          </div>

          <nav
            aria-label="Primary"
            className="flex flex-col gap-1 md:h-full md:flex-row md:items-stretch md:gap-9"
          >
            <NavItem to="/" label="Pokémon catalog" end />
            <NavItem to="/collection" label="Collection" />
          </nav>

          <div className="flex flex-col gap-3 md:ml-auto md:flex-row md:items-center md:gap-6">
            {isAuthenticated ? (
              <div className="flex flex-wrap items-center gap-3 md:gap-6">
                <div className="flex items-center gap-2">
                  <img
                    src={accountIcon}
                    alt=""
                    width={20}
                    height={20}
                    className="size-5"
                  />
                  <span className="text-body-sm font-medium">
                    {user?.name ?? 'Account'}
                  </span>
                </div>
                <Button
                  type="button"
                  variant="ghost"
                  className="h-11 px-3 text-body-sm text-nav-foreground hover:bg-white/10 hover:text-nav-foreground"
                  onClick={() => {
                    signOut()
                  }}
                >
                  Logout
                </Button>
              </div>
            ) : (
              <div className="flex gap-3 md:gap-6">
                <Button
                  asChild
                  variant="outline"
                  className="h-11 flex-1 border-white/15 bg-transparent text-body-sm text-nav-foreground hover:bg-white/10 hover:text-nav-foreground md:flex-none md:border-transparent md:px-3"
                >
                  <Link to="/login">Sign in</Link>
                </Button>
                <Button
                  asChild
                  className="h-11 flex-1 rounded-md text-body-sm md:flex-none"
                >
                  <Link to="/register">Create account</Link>
                </Button>
              </div>
            )}
          </div>
        </div>
      </header>

      <main ref={mainRef} className="flex-1">
        <Outlet />
      </main>

      <footer className="border-t border-border bg-card px-6 py-6 text-muted-foreground md:px-14">
        <div className="mx-auto flex w-full max-w-6xl flex-col gap-3.5">
          <div className="flex items-center justify-between gap-4">
            <p className="text-caption">Explore. Record. Collect.</p>
            <p className="font-mono text-label tracking-wide uppercase">
              All generations
            </p>
          </div>
          <p className="text-label leading-normal">
            Data & sprites: PokéAPI. Pokémon © Nintendo / Creatures Inc. / GAME
            FREAK inc.
          </p>
        </div>
      </footer>

      <SessionExpiredDialog />
    </div>
  )
}
