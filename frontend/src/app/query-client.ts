import {
  keepPreviousData,
  MutationCache,
  QueryClient,
} from '@tanstack/react-query'

import { ApiError } from '@/api/api-error'
import { openSessionExpired } from '@/features/auth/session-expired'

export const STALE_TIME_MS = 5 * 60 * 1000

/** Shared option shape for paginated list queries (catalog / collection). */
export const listQueryOptions = {
  placeholderData: keepPreviousData,
} as const

export function queryRetry(failureCount: number, error: unknown): boolean {
  if (!(error instanceof ApiError)) {
    return false
  }
  if (error.status >= 400 && error.status < 500) {
    return false
  }
  if (error.status === 0 || error.status === 502) {
    return failureCount < 1
  }
  return false
}

let signOutHandler: () => void = () => {}

/** Wired from AuthProvider via AppProviders so MutationCache can clear session. */
export function setQueryClientSignOut(signOut: () => void): void {
  signOutHandler = signOut
}

export type CreateAppQueryClientOptions = {
  signOut?: () => void
}

export function createAppQueryClient(
  options: CreateAppQueryClientOptions = {},
): QueryClient {
  if (options.signOut) {
    setQueryClientSignOut(options.signOut)
  }

  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: STALE_TIME_MS,
        retry: queryRetry,
      },
      mutations: {
        retry: false,
      },
    },
    mutationCache: new MutationCache({
      onError: (error, _variables, _onMutateResult, mutation) => {
        if (
          error instanceof ApiError &&
          error.status === 401 &&
          mutation.meta?.protectedWrite === true
        ) {
          signOutHandler()
          openSessionExpired()
        }
      },
    }),
  })
}
