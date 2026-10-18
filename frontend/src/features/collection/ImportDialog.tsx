import { CheckCircle2, CopyCheck, Loader2, X } from 'lucide-react'
import { Link } from 'react-router'

import type { PokemonDetail } from '@/api/types'
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

export type ImportDialogStep =
  | 'confirm'
  | 'progress'
  | 'success'
  | 'duplicate'
  | 'error'

export type ImportDialogProps = {
  open: boolean
  step: ImportDialogStep
  pokemon: Pick<
    PokemonDetail,
    'nationalNumber' | 'name' | 'category' | 'imageUrl'
  >
  errorDetail?: string | null
  onOpenChange: (open: boolean) => void
  onConfirm: () => void
  onRetry: () => void
}

function PokemonSummary({
  pokemon,
}: {
  pokemon: ImportDialogProps['pokemon']
}) {
  return (
    <div className="flex w-full items-center gap-4 rounded-[10px] border border-border bg-surface p-3">
      <div className="flex size-16 shrink-0 items-center justify-center">
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
          {formatNationalNumber(pokemon.nationalNumber)} · National Pokédex
        </p>
        <p className="text-caption text-muted-foreground">{pokemon.category}</p>
      </div>
    </div>
  )
}

const dialogButtonClass =
  'h-12 min-h-12 w-full rounded-[10px] px-4 text-[0.8125rem] font-semibold sm:w-auto'

export function ImportDialog({
  open,
  step,
  pokemon,
  errorDetail,
  onOpenChange,
  onConfirm,
  onRetry,
}: ImportDialogProps) {
  const busy = step === 'progress'
  const collectionHref = `/collection/${pokemon.nationalNumber}`

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
        {step === 'confirm' ? (
          <>
            <AlertDialogHeader className="gap-5">
              <div className="flex items-center gap-2">
                <p className="flex-1 font-mono text-label tracking-wide text-primary uppercase">
                  Collection / Import Pokémon
                </p>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="size-11 shrink-0"
                  aria-label="Dismiss dialog"
                  onClick={() => onOpenChange(false)}
                >
                  <X className="size-5" aria-hidden />
                </Button>
              </div>
              <AlertDialogTitle>Add to the collection?</AlertDialogTitle>
              <PokemonSummary pokemon={pokemon} />
              <AlertDialogDescription className="space-y-3 text-foreground">
                <span className="block">
                  A copy of {pokemon.name}&apos;s Pokédex record will be added
                  to the shared collection.
                </span>
                <span className="block text-muted-foreground">
                  The public catalog is not affected. You can add your own name,
                  location and tags after saving.
                </span>
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter className="flex-col-reverse sm:flex-row">
              <AlertDialogCancel className={dialogButtonClass}>
                Cancel
              </AlertDialogCancel>
              <Button
                type="button"
                className={dialogButtonClass}
                onClick={onConfirm}
              >
                Add to collection
              </Button>
            </AlertDialogFooter>
          </>
        ) : null}

        {step === 'progress' ? (
          <>
            <AlertDialogHeader className="gap-5">
              <p className="font-mono text-label tracking-wide text-primary uppercase">
                Collection / Saving
              </p>
              <div className="flex items-center gap-3 text-muted-foreground">
                <Loader2 className="size-6 animate-spin text-primary" aria-hidden />
                <p className="text-[0.8125rem] font-medium">Please wait</p>
              </div>
              <AlertDialogTitle>Adding to the collection…</AlertDialogTitle>
              <PokemonSummary pokemon={pokemon} />
              <AlertDialogDescription>
                We&apos;re adding {pokemon.name} to the shared collection. This
                may take a moment.
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <Button
                type="button"
                disabled
                className={`${dialogButtonClass} border border-border bg-muted text-muted-foreground hover:bg-muted`}
              >
                Saving…
              </Button>
            </AlertDialogFooter>
          </>
        ) : null}

        {step === 'success' ? (
          <>
            <AlertDialogHeader className="gap-5">
              <div className="flex items-center gap-2">
                <p className="flex-1 font-mono text-label tracking-wide text-primary uppercase">
                  Collection / Entry added
                </p>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="size-11 shrink-0"
                  aria-label="Dismiss dialog"
                  onClick={() => onOpenChange(false)}
                >
                  <X className="size-5" aria-hidden />
                </Button>
              </div>
              <div className="flex items-center gap-2 text-success">
                <CheckCircle2 className="size-6" aria-hidden />
                <p className="text-caption font-semibold">
                  Saved to the shared collection
                </p>
              </div>
              <AlertDialogTitle>Added to the collection</AlertDialogTitle>
              <PokemonSummary pokemon={pokemon} />
              <AlertDialogDescription>
                {pokemon.name} is now in the shared collection. Open its details
                to view the saved record or add personal details.
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel className={dialogButtonClass}>
                Close
              </AlertDialogCancel>
              <Button asChild className={dialogButtonClass}>
                <Link to={collectionHref} onClick={() => onOpenChange(false)}>
                  View in the collection
                </Link>
              </Button>
            </AlertDialogFooter>
          </>
        ) : null}

        {step === 'duplicate' ? (
          <>
            <AlertDialogHeader className="gap-5">
              <div className="flex items-center gap-2">
                <p className="flex-1 font-mono text-label tracking-wide text-primary uppercase">
                  Collection / Existing entry
                </p>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="size-11 shrink-0"
                  aria-label="Dismiss dialog"
                  onClick={() => onOpenChange(false)}
                >
                  <X className="size-5" aria-hidden />
                </Button>
              </div>
              <div className="flex items-center gap-2 text-[#886321]">
                <CopyCheck className="size-6" aria-hidden />
                <p className="text-caption font-semibold">No duplicate added</p>
              </div>
              <AlertDialogTitle>Already in the collection</AlertDialogTitle>
              <PokemonSummary pokemon={pokemon} />
              <AlertDialogDescription>
                {pokemon.name} is already saved in the shared collection. You
                can edit its name, location and tags instead.
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel className={dialogButtonClass}>
                Close
              </AlertDialogCancel>
              <Button asChild className={dialogButtonClass}>
                <Link to={collectionHref} onClick={() => onOpenChange(false)}>
                  View in the collection
                </Link>
              </Button>
            </AlertDialogFooter>
          </>
        ) : null}

        {step === 'error' ? (
          <>
            <AlertDialogHeader className="gap-5">
              <div className="flex items-center gap-2">
                <p className="flex-1 font-mono text-label tracking-wide text-primary uppercase">
                  Collection / Import failed
                </p>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="size-11 shrink-0"
                  aria-label="Dismiss dialog"
                  onClick={() => onOpenChange(false)}
                >
                  <X className="size-5" aria-hidden />
                </Button>
              </div>
              <AlertDialogTitle>Could not add to the collection</AlertDialogTitle>
              <AlertDialogDescription>
                {errorDetail ??
                  'Something went wrong while saving. Check your connection and try again.'}
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel className={dialogButtonClass}>
                Close
              </AlertDialogCancel>
              <Button
                type="button"
                className={dialogButtonClass}
                onClick={onRetry}
              >
                Retry
              </Button>
            </AlertDialogFooter>
          </>
        ) : null}
      </AlertDialogContent>
    </AlertDialog>
  )
}
