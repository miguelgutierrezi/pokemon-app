import { X } from 'lucide-react'

import type { LocalPokemon } from '@/api/types'
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Button } from '@/components/ui/button'
import { formatNationalNumber } from '@/lib/format'

export type RemoveDialogProps = {
  open: boolean
  pokemon: Pick<
    LocalPokemon,
    'nationalNumber' | 'name' | 'imageUrl' | 'localizedName'
  > | null
  busy?: boolean
  onOpenChange: (open: boolean) => void
  onConfirm: () => void
}

const dialogButtonClass =
  'h-11 min-h-11 w-full rounded-[10px] px-4 text-[0.8125rem] font-semibold sm:w-auto'

export function RemoveDialog({
  open,
  pokemon,
  busy = false,
  onOpenChange,
  onConfirm,
}: RemoveDialogProps) {
  const handleOpenChange = (next: boolean) => {
    if (busy && !next) {
      return
    }
    onOpenChange(next)
  }

  return (
    <AlertDialog open={open} onOpenChange={handleOpenChange}>
      <AlertDialogContent
        className="gap-5"
        onEscapeKeyDown={(event) => {
          if (busy) {
            event.preventDefault()
          }
        }}
      >
        <AlertDialogHeader className="gap-5">
          <div className="flex items-center gap-2">
            <p className="flex-1 font-mono text-label tracking-wide text-primary uppercase">
              Collection / Remove entry
            </p>
            <Button
              type="button"
              variant="ghost"
              size="icon"
              className="size-11 shrink-0"
              aria-label="Dismiss dialog"
              disabled={busy}
              onClick={() => onOpenChange(false)}
            >
              <X className="size-5" aria-hidden />
            </Button>
          </div>
          <AlertDialogTitle>Remove from collection?</AlertDialogTitle>
          {pokemon ? (
            <div className="flex w-full items-center gap-4 rounded-[10px] border border-border bg-surface p-3">
              <div className="flex size-16 shrink-0 items-center justify-center overflow-hidden rounded-[10px] bg-surface">
                <img
                  src={pokemon.imageUrl}
                  alt=""
                  width={46}
                  height={44}
                  className="max-h-11 max-w-[46px] object-contain"
                />
              </div>
              <div className="min-w-0 space-y-1.5">
                <p className="text-[0.9375rem] font-semibold text-foreground">
                  {pokemon.name}
                </p>
                <p className="font-mono text-label tracking-wide text-muted-foreground uppercase">
                  {formatNationalNumber(pokemon.nationalNumber)} · National
                  Pokédex
                </p>
                <p className="text-caption text-muted-foreground">
                  Localized name:{' '}
                  {pokemon.localizedName?.trim()
                    ? pokemon.localizedName
                    : '—'}
                </p>
              </div>
            </div>
          ) : null}
          <AlertDialogDescription className="space-y-3 text-left">
            <span className="block text-foreground">
              This removes {pokemon?.name ?? 'this Pokémon'} from the shared
              collection, along with the localized name, location and tags.
            </span>
            <span className="block text-muted-foreground">
              The public catalog is not affected. You can still view{' '}
              {pokemon?.name ?? 'it'} there. Personal data cannot be restored.
            </span>
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel className={dialogButtonClass} disabled={busy}>
            Cancel
          </AlertDialogCancel>
          <Button
            type="button"
            className={dialogButtonClass}
            disabled={busy}
            onClick={onConfirm}
          >
            {busy ? 'Removing…' : 'Remove from collection'}
          </Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  )
}
