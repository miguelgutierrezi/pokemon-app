import { useParams } from 'react-router'

import { PageHeading } from '@/components/page-heading'

export function PokemonDetailPage() {
  const { id } = useParams()
  return (
    <div className="mx-auto max-w-6xl px-6 py-10">
      <PageHeading>Pokémon {id}</PageHeading>
      <p className="mt-2 text-body text-muted-foreground">Detail coming soon.</p>
    </div>
  )
}
