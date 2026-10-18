import { useQuery } from '@tanstack/react-query'
import { useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router'

import { listPokemon } from '@/api/pokemon'
import { listQueryOptions } from '@/app/query-client'
import {
  CATALOG_PAGE_SIZE,
  catalogListQueryKey,
} from '@/features/catalog/query-keys'
import { isBeyondLastPageError, parsePageParam } from '@/lib/page-param'

export { isBeyondLastPageError } from '@/lib/page-param'

export function useCatalogPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const rawPage = searchParams.get('page')
  const parsedPage = parsePageParam(rawPage)
  const pageInvalid = parsedPage == null
  const page = parsedPage ?? 1

  useEffect(() => {
    if (pageInvalid) {
      navigate({ pathname: '/', search: '?page=1' }, { replace: true })
    }
  }, [pageInvalid, navigate])

  const query = useQuery({
    queryKey: catalogListQueryKey({ page, size: CATALOG_PAGE_SIZE }),
    queryFn: ({ signal }) =>
      listPokemon({ page, size: CATALOG_PAGE_SIZE }, signal),
    enabled: !pageInvalid,
    ...listQueryOptions,
  })

  const beyondLast = isBeyondLastPageError(query.error, page)

  return {
    page,
    pageSize: CATALOG_PAGE_SIZE,
    pageInvalid,
    beyondLast,
    ...query,
  }
}
