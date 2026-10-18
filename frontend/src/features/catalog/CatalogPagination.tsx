import { Link } from 'react-router'

import { buttonVariants } from '@/components/ui/button'
import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
} from '@/components/ui/pagination'
import { cn } from '@/lib/utils'

function pageHref(page: number): string {
  return `/?page=${page}`
}

function buildPages(current: number, total: number): Array<number | 'ellipsis'> {
  if (total <= 7) {
    return Array.from({ length: total }, (_, index) => index + 1)
  }
  const pages: Array<number | 'ellipsis'> = [1]
  const start = Math.max(2, current - 1)
  const end = Math.min(total - 1, current + 1)
  if (start > 2) {
    pages.push('ellipsis')
  }
  for (let page = start; page <= end; page += 1) {
    pages.push(page)
  }
  if (end < total - 1) {
    pages.push('ellipsis')
  }
  pages.push(total)
  return pages
}

export function CatalogPagination({
  page,
  totalPages,
}: {
  page: number
  totalPages: number
}) {
  if (totalPages <= 1) {
    return null
  }

  const items = buildPages(page, totalPages)

  return (
    <div className="mt-10 space-y-3">
      <Pagination>
        <PaginationContent>
          <PaginationItem>
            {page > 1 ? (
              <Link
                to={pageHref(page - 1)}
                className={cn(buttonVariants({ variant: 'ghost', size: 'default' }), 'gap-1')}
                aria-label="Go to previous page"
              >
                Previous
              </Link>
            ) : (
              <span
                className={cn(
                  buttonVariants({ variant: 'ghost', size: 'default' }),
                  'pointer-events-none gap-1 opacity-40',
                )}
                aria-disabled
              >
                Previous
              </span>
            )}
          </PaginationItem>
          {items.map((item, index) =>
            item === 'ellipsis' ? (
              <PaginationItem key={`e-${index}`}>
                <PaginationEllipsis />
              </PaginationItem>
            ) : (
              <PaginationItem key={item}>
                <Link
                  to={pageHref(item)}
                  aria-current={item === page ? 'page' : undefined}
                  className={cn(
                    buttonVariants({
                      variant: item === page ? 'default' : 'ghost',
                      size: 'icon',
                    }),
                  )}
                >
                  {item}
                </Link>
              </PaginationItem>
            ),
          )}
          <PaginationItem>
            {page < totalPages ? (
              <Link
                to={pageHref(page + 1)}
                className={cn(buttonVariants({ variant: 'ghost', size: 'default' }), 'gap-1')}
                aria-label="Go to next page"
              >
                Next
              </Link>
            ) : (
              <span
                className={cn(
                  buttonVariants({ variant: 'ghost', size: 'default' }),
                  'pointer-events-none gap-1 opacity-40',
                )}
                aria-disabled
              >
                Next
              </span>
            )}
          </PaginationItem>
        </PaginationContent>
      </Pagination>
      <p className="text-center font-mono text-label tracking-wide text-muted-foreground uppercase">
        Page {page} of {totalPages} · 9 per page
      </p>
    </div>
  )
}
