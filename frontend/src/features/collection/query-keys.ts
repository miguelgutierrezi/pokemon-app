import type { PageQuery } from '@/api/types'

export const COLLECTION_PAGE_SIZE = 9

export function collectionListQueryKey(query: PageQuery) {
  return ['collection', 'list', { page: query.page, size: query.size }] as const
}
