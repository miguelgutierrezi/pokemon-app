import { useQueryClient } from '@tanstack/react-query'
import { KeyRound, X } from 'lucide-react'
import { useSyncExternalStore } from 'react'
import { useLocation, useNavigate } from 'react-router'

import type { LocalPokemon, PokemonDetail } from '@/api/types'
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Button } from '@/components/ui/button'
import {
  buildSessionExpiredContext,
  sessionExpiredEntityId,
} from '@/features/auth/session-expired-context'
import {
  closeSessionExpired,
  getSessionExpiredOpen,
  subscribeSessionExpired,
} from '@/features/auth/session-expired'
import { localPokemonQueryKey } from '@/features/collection/query-keys'
import { catalogPokemonQueryKey } from '@/features/pokemon/query-keys'
import { sanitizeReturnPath } from '@/lib/return-path'

const dialogButtonClass =
  'h-11 min-h-11 w-full rounded-[10px] px-4 text-[0.8125rem] font-semibold sm:w-auto'

function resolveEntityName(
  queryClient: ReturnType<typeof useQueryClient>,
  id: number | null,
): string | null {
  if (id == null) {
    return null
  }
  const detail = queryClient.getQueryData<PokemonDetail>(
    catalogPokemonQueryKey(id),
  )
  if (detail?.name) {
    return detail.name
  }
  const local = queryClient.getQueryData<LocalPokemon>(localPokemonQueryKey(id))
  return local?.name ?? null
}

export function SessionExpiredDialog() {
  const open = useSyncExternalStore(
    subscribeSessionExpired,
    getSessionExpiredOpen,
    () => false,
  )
  const location = useLocation()
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const entityId = sessionExpiredEntityId(location.pathname)
  const entityName = resolveEntityName(queryClient, entityId)
  const contextLabel = buildSessionExpiredContext(
    location.pathname,
    location.search,
    entityName,
  )
  const returnPath = sanitizeReturnPath(
    `${location.pathname}${location.search}`,
  )
  const loginHref = `/login?redirect=${encodeURIComponent(returnPath)}&reason=expired`

  const close = () => {
    closeSessionExpired()
  }

  return (
    <AlertDialog
      open={open}
      onOpenChange={(next) => {
        if (!next) {
          close()
        }
      }}
    >
      <AlertDialogContent className="gap-5">
        <AlertDialogHeader className="gap-5">
          <div className="flex items-center gap-2">
            <p className="flex-1 font-mono text-label tracking-wide text-primary uppercase">
              Account / Session expired
            </p>
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="size-11 shrink-0"
              aria-label="Dismiss dialog"
              onClick={close}
            >
              <X className="size-5" aria-hidden />
            </Button>
          </div>

          <div className="flex size-12 items-center justify-center rounded-[10px] bg-surface">
            <KeyRound className="size-6 text-primary" aria-hidden />
          </div>

          <AlertDialogTitle>Your session has expired</AlertDialogTitle>

          <p className="rounded-[10px] border border-border bg-surface px-3.5 py-2.5 text-[0.8125rem] font-medium text-foreground">
            {contextLabel}
          </p>

          <AlertDialogDescription className="space-y-3 text-left">
            <span className="block text-foreground">
              {entityName
                ? `Sign in again to continue saving ${entityName} to the shared collection.`
                : 'Sign in again to continue where you left off.'}
            </span>
            <span className="block text-muted-foreground">
              You can still explore the public catalog without signing in.
            </span>
          </AlertDialogDescription>
        </AlertDialogHeader>

        <AlertDialogFooter className="flex-col-reverse gap-2.5 sm:flex-row sm:justify-end">
          <Button
            type="button"
            variant="outline"
            className={dialogButtonClass}
            onClick={() => {
              close()
              void navigate('/')
            }}
          >
            Browse catalog
          </Button>
          <Button
            type="button"
            className={dialogButtonClass}
            onClick={() => {
              close()
              void navigate(loginHref)
            }}
          >
            Sign in again
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
