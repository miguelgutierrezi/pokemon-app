import { useParams } from 'react-router'

import { PageHeading } from '@/components/page-heading'

export function CollectionEditPage() {
  const { id } = useParams()
  return (
    <div className="mx-auto max-w-6xl px-6 py-10">
      <PageHeading>Edit collection entry {id}</PageHeading>
      <p className="mt-2 text-body text-muted-foreground">Edit coming soon.</p>
    </div>
  )
}
