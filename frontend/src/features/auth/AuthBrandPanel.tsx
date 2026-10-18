import { Link } from 'react-router'

import pokeballArchiveDiagram from '@/assets/pokeball-archive-diagram.svg'
import pokeballEmblem from '@/assets/pokeball-emblem.svg'

export function AuthBrandPanel() {
  return (
    <aside className="flex w-full flex-col justify-between bg-nav p-6 text-nav-foreground md:p-8 lg:w-[min(39rem,43vw)] lg:p-14">
      <Link to="/" className="flex items-center gap-3 text-nav-foreground">
        <img
          src={pokeballEmblem}
          alt=""
          width={34}
          height={34}
          className="size-7 lg:size-[2.125rem]"
        />
        <span className="text-[1.375rem] font-semibold lg:text-display">
          Pokéfolio
        </span>
      </Link>

      <div className="mt-6 flex flex-col gap-4 lg:mt-0 lg:gap-8">
        <div className="space-y-3 lg:space-y-4">
          <div className="flex items-center gap-2.5">
            <span className="h-0.5 w-4 bg-primary lg:h-[3px] lg:w-5" aria-hidden />
            <p className="font-mono text-[0.5625rem] tracking-wide text-nav-muted uppercase lg:text-caption">
              Your Pokédex record
            </p>
          </div>
          <div className="flex items-center gap-3 lg:block">
            <p className="flex-1 text-2xl leading-tight font-semibold lg:text-hero lg:leading-[1.16]">
              Every Pokémon,
              <br />
              a new story.
            </p>
            <img
              src={pokeballArchiveDiagram}
              alt=""
              width={134}
              height={134}
              className="size-[5.125rem] shrink-0 lg:mt-8 lg:size-[19.5rem] lg:w-auto"
            />
          </div>
          <p className="max-w-md text-body-sm leading-relaxed text-nav-muted lg:text-[0.9375rem] lg:leading-[1.6]">
            Explore the catalog and shape the shared Pokémon collection.
          </p>
        </div>
      </div>

      <div className="mt-6 hidden items-center justify-between text-nav-muted lg:mt-0 lg:flex">
        <p className="text-caption">Explore. Record. Collect.</p>
        <p className="font-mono text-label tracking-wide uppercase">
          All generations
        </p>
      </div>
    </aside>
  )
}
