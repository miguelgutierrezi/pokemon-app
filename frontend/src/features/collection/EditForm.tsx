import { useId, useRef, useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'

import { ApiError } from '@/api/api-error'
import type { LocalPokemon } from '@/api/types'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  commitPendingTag,
  firstInvalidEditField,
  toUpdateBody,
  validateEditForm,
  type EditFieldErrors,
} from '@/features/collection/edit-validation'
import { RemoveDialog } from '@/features/collection/RemoveDialog'
import { TagInput } from '@/features/collection/TagInput'
import { useDeleteLocalPokemon } from '@/features/collection/useDeleteLocalPokemon'
import { useUpdateLocalPokemon } from '@/features/collection/useUpdateLocalPokemon'
import { cn } from '@/lib/utils'

export type EditFormProps = {
  pokemon: LocalPokemon
  readOnly: boolean
  loginHref: string
  onNotFound: () => void
  onReload: () => Promise<LocalPokemon>
}

export function EditForm({
  pokemon,
  readOnly,
  loginHref,
  onNotFound,
  onReload,
}: EditFormProps) {
  const navigate = useNavigate()
  const updateMutation = useUpdateLocalPokemon()
  const deleteMutation = useDeleteLocalPokemon()

  const [localizedName, setLocalizedName] = useState(
    pokemon.localizedName ?? '',
  )
  const [location, setLocation] = useState(pokemon.location ?? '')
  const [tags, setTags] = useState(pokemon.tags)
  const [tagDraft, setTagDraft] = useState('')
  const version = pokemon.version
  const [fieldErrors, setFieldErrors] = useState<EditFieldErrors>({})
  const [stale, setStale] = useState(false)
  const [removeOpen, setRemoveOpen] = useState(false)

  const nameRef = useRef<HTMLInputElement>(null)
  const locationRef = useRef<HTMLInputElement>(null)
  const tagsRef = useRef<HTMLInputElement>(null)
  const nameErrorId = useId()
  const locationErrorId = useId()
  const tagsErrorId = useId()

  function focusField(field: keyof EditFieldErrors) {
    const map = {
      localizedName: nameRef,
      location: locationRef,
      tags: tagsRef,
    } as const
    map[field].current?.focus()
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (readOnly) {
      return
    }

    const pending = commitPendingTag(tags, tagDraft)
    if (pending.error) {
      setTags(pending.tags)
      setTagDraft(pending.draft)
      setFieldErrors({ tags: pending.error })
      queueMicrotask(() => focusField('tags'))
      return
    }

    const nextTags = pending.tags
    setTags(nextTags)
    setTagDraft(pending.draft)

    const clientErrors = validateEditForm({
      localizedName,
      location,
      tags: nextTags,
    })
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors)
      const first = firstInvalidEditField(clientErrors)
      if (first) {
        queueMicrotask(() => focusField(first))
      }
      return
    }

    setFieldErrors({})
    setStale(false)

    try {
      await updateMutation.mutateAsync({
        id: pokemon.nationalNumber,
        body: toUpdateBody({
          localizedName,
          location,
          tags: nextTags,
          version,
        }),
      })
      void navigate('/collection')
    } catch (error) {
      if (!(error instanceof ApiError)) {
        setFieldErrors({ localizedName: 'Unexpected error. Try again.' })
        return
      }
      if (error.status === 401) {
        return
      }
      if (error.status === 404) {
        onNotFound()
        return
      }
      if (
        error.status === 409 &&
        error.type === 'urn:problem-type:stale-version'
      ) {
        setStale(true)
        return
      }
      if (error.status === 400 && error.field) {
        const field = error.field as keyof EditFieldErrors
        if (
          field === 'localizedName' ||
          field === 'location' ||
          field === 'tags'
        ) {
          setFieldErrors({ [field]: error.detail })
          queueMicrotask(() => focusField(field))
          return
        }
      }
      setFieldErrors({ localizedName: error.detail })
    }
  }

  async function handleReload() {
    await onReload()
  }

  async function handleConfirmRemove() {
    try {
      await deleteMutation.mutateAsync({
        id: pokemon.nationalNumber,
        name: pokemon.name,
      })
      setRemoveOpen(false)
      void navigate('/collection')
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) {
        setRemoveOpen(false)
        void navigate('/collection')
        return
      }
      if (error instanceof ApiError && error.status === 401) {
        setRemoveOpen(false)
      }
    }
  }

  return (
    <>
      <form
        className="flex flex-col gap-6 rounded-xl border border-border bg-card p-6 md:p-8"
        onSubmit={(event) => void handleSubmit(event)}
        noValidate
      >
        <div className="space-y-2.5">
          <p className="font-mono text-label tracking-wide text-primary uppercase">
            Shared collection / Edit
          </p>
          <h2 className="text-title font-semibold text-foreground">
            Collection details
          </h2>
          <p className="text-[0.8125rem] leading-[1.6] text-muted-foreground">
            Localized names, locations and tags are separate from the official
            Pokédex data.
          </p>
        </div>

        {stale ? (
          <Alert className="rounded-md border-danger-border bg-danger-muted">
            <AlertTitle>Modified elsewhere — reload</AlertTitle>
            <AlertDescription>
              Someone else saved this record. Reload to see the latest values,
              then try again. Your unsaved edits will not be kept.
            </AlertDescription>
            <Button
              type="button"
              className="mt-4"
              onClick={() => void handleReload()}
            >
              Reload
            </Button>
          </Alert>
        ) : null}

        {fieldErrors.tags && updateMutation.isError ? (
          <Alert className="rounded-md border-l-[3px] border-primary bg-danger-muted">
            <AlertTitle>Check the Tags field</AlertTitle>
            <AlertDescription>
              Your changes weren’t saved. Correct the format below and try
              again.
            </AlertDescription>
          </Alert>
        ) : null}

        <div className="grid gap-6 md:grid-cols-2">
          <div className="space-y-2">
            <Label htmlFor="localizedName">Localized name</Label>
            <Input
              ref={nameRef}
              id="localizedName"
              value={localizedName}
              disabled={readOnly}
              aria-invalid={fieldErrors.localizedName ? true : undefined}
              aria-describedby={
                fieldErrors.localizedName ? nameErrorId : undefined
              }
              className={cn(
                'h-12 rounded-[10px]',
                fieldErrors.localizedName && 'border-primary',
              )}
              onChange={(event) => setLocalizedName(event.target.value)}
            />
            {fieldErrors.localizedName ? (
              <p id={nameErrorId} className="text-[0.6875rem] text-primary" role="alert">
                {fieldErrors.localizedName}
              </p>
            ) : (
              <p className="text-[0.6875rem] text-muted-foreground">
                The name you use in the shared collection.
              </p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="location">Location</Label>
            <Input
              ref={locationRef}
              id="location"
              value={location}
              disabled={readOnly}
              aria-invalid={fieldErrors.location ? true : undefined}
              aria-describedby={
                fieldErrors.location ? locationErrorId : undefined
              }
              className={cn(
                'h-12 rounded-[10px]',
                fieldErrors.location && 'border-primary',
              )}
              onChange={(event) => setLocation(event.target.value)}
            />
            {fieldErrors.location ? (
              <p
                id={locationErrorId}
                className="text-[0.6875rem] text-primary"
                role="alert"
              >
                {fieldErrors.location}
              </p>
            ) : (
              <p className="text-[0.6875rem] text-muted-foreground">
                Your own location or context, not the species’ region.
              </p>
            )}
          </div>
        </div>

        <TagInput
          tags={tags}
          draft={tagDraft}
          disabled={readOnly}
          invalid={Boolean(fieldErrors.tags)}
          errorId={tagsErrorId}
          errorMessage={fieldErrors.tags}
          inputRef={tagsRef}
          onDraftChange={setTagDraft}
          onTagsChange={setTags}
        />

        <div className="space-y-5 border-t border-border pt-6">
          <p className="text-[0.6875rem] leading-[1.6] text-muted-foreground">
            Shared collection · saved on the server.
          </p>

          {readOnly ? (
            <Button asChild className="h-12 rounded-[10px]">
              <Link to={loginHref}>Sign in to edit</Link>
            </Button>
          ) : (
            <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-between">
              <Button
                type="button"
                variant="outline"
                className="h-12 rounded-[10px] text-primary"
                onClick={() => setRemoveOpen(true)}
              >
                Remove from collection
              </Button>
              <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
                <Button
                  type="button"
                  variant="outline"
                  className="h-12 min-w-[104px] rounded-[10px]"
                  asChild
                >
                  <Link to="/collection">Cancel</Link>
                </Button>
                <Button
                  type="submit"
                  className="h-12 min-w-[158px] rounded-[10px]"
                  disabled={updateMutation.isPending}
                >
                  {updateMutation.isPending ? 'Saving…' : 'Save changes'}
                </Button>
              </div>
            </div>
          )}
        </div>
      </form>

      <RemoveDialog
        open={removeOpen}
        pokemon={pokemon}
        busy={deleteMutation.isPending}
        onOpenChange={setRemoveOpen}
        onConfirm={() => void handleConfirmRemove()}
      />
    </>
  )
}
