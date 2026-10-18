import type { PageQuery } from '@/api/types'

export const CATALOG_PAGE_SIZE = 9

export function catalogListQueryKey(query: PageQuery) {
  return ['pokemon', 'list', { page: query.page, size: query.size }] as const
}
