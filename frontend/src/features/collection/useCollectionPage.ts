import { useQuery } from '@tanstack/react-query'
import { useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router'

import { listCollection } from '@/api/collection'
import { listQueryOptions } from '@/app/query-client'
import {
  COLLECTION_PAGE_SIZE,
  collectionListQueryKey,
} from '@/features/collection/query-keys'
import { isBeyondLastPageError, parsePageParam } from '@/lib/page-param'

export function useCollectionPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const rawPage = searchParams.get('page')
  const parsedPage = parsePageParam(rawPage)
  const pageInvalid = parsedPage == null
  const page = parsedPage ?? 1

  useEffect(() => {
    if (pageInvalid) {
      navigate(
        { pathname: '/collection', search: '?page=1' },
        { replace: true },
      )
    }
  }, [pageInvalid, navigate])

  const query = useQuery({
    queryKey: collectionListQueryKey({ page, size: COLLECTION_PAGE_SIZE }),
    queryFn: ({ signal }) =>
      listCollection({ page, size: COLLECTION_PAGE_SIZE }, signal),
    enabled: !pageInvalid,
    ...listQueryOptions,
  })

  const beyondLast = isBeyondLastPageError(query.error, page)

  return {
    page,
    pageSize: COLLECTION_PAGE_SIZE,
    pageInvalid,
    beyondLast,
    ...query,
  }
}
