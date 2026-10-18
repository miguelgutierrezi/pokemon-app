import { useQuery } from '@tanstack/react-query'
import { useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router'

import { ApiError } from '@/api/api-error'
import { listPokemon } from '@/api/pokemon'
import { listQueryOptions } from '@/app/query-client'
import {
  CATALOG_PAGE_SIZE,
  catalogListQueryKey,
} from '@/features/catalog/query-keys'

function parsePage(raw: string | null): number | null {
  if (raw == null || raw.trim() === '') {
    return 1
  }
  if (!/^\d+$/.test(raw)) {
    return null
  }
  const value = Number(raw)
  if (!Number.isInteger(value) || value < 1) {
    return null
  }
  return value
}

/** Same contract as collection list: API 400 when page is past the last page. */
export function isBeyondLastPageError(
  error: unknown,
  page: number,
): boolean {
  return (
    page > 1 &&
    error instanceof ApiError &&
    error.status === 400
  )
}

export function useCatalogPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const rawPage = searchParams.get('page')
  const parsedPage = parsePage(rawPage)
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
