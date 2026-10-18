import { useEffect, useRef } from 'react'
import { Link, Outlet, useLocation } from 'react-router'

import pokeballEmblem from '@/assets/pokeball-emblem.svg'

/**
 * Split-screen chrome for /login and /register (Figma 5-233 / 5-921).
 * Left brand panel is a placeholder until T025–T028.
 */
export function AuthLayout() {
  const location = useLocation()
  const mainRef = useRef<HTMLElement>(null)

  useEffect(() => {
    const heading = mainRef.current?.querySelector('h1')
    heading?.focus()
  }, [location.pathname, location.search])

  return (
    <div className="flex min-h-screen bg-card text-foreground">
      <aside className="hidden w-[min(39rem,43vw)] flex-col justify-between bg-nav p-14 text-nav-foreground lg:flex">
        <Link to="/" className="flex items-center gap-3 text-nav-foreground">
          <img
            src={pokeballEmblem}
            alt=""
            width={32}
            height={32}
            className="size-8"
          />
          <span className="text-display font-semibold">Pokéfolio</span>
        </Link>
        <div className="space-y-4">
          <div className="flex items-center gap-2.5">
            <span className="h-0.5 w-5 bg-primary" aria-hidden />
            <p className="font-mono text-caption tracking-wide text-nav-muted uppercase">
              Your Pokédex record
            </p>
          </div>
          <p className="text-hero leading-tight font-semibold">
            Every Pokémon,
            <br />
            a new story.
          </p>
          <p className="max-w-md text-body leading-relaxed text-nav-muted">
            Explore the catalog and shape your own Pokémon collection.
          </p>
        </div>
        <div className="flex items-center justify-between text-nav-muted">
          <p className="text-caption">Explore. Record. Collect.</p>
          <p className="font-mono text-label tracking-wide uppercase">
            All generations
          </p>
        </div>
      </aside>

      <main
        ref={mainRef}
        className="flex flex-1 flex-col items-center justify-center px-6 py-10"
      >
        <Outlet />
      </main>
    </div>
  )
}
