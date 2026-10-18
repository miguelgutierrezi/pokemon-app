import { X } from 'lucide-react'
import { useId, type KeyboardEvent, type Ref } from 'react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  appendTagsFromCommaInput,
  commitPendingTag,
  TAG_MAX_LENGTH,
  TAGS_MAX,
} from '@/features/collection/edit-validation'
import { cn } from '@/lib/utils'

export type TagInputProps = {
  tags: string[]
  draft: string
  disabled?: boolean
  invalid?: boolean
  errorId?: string
  errorMessage?: string
  inputRef?: Ref<HTMLInputElement>
  onDraftChange: (value: string) => void
  onTagsChange: (tags: string[]) => void
}

export function TagInput({
  tags,
  draft,
  disabled = false,
  invalid = false,
  errorId,
  errorMessage,
  inputRef,
  onDraftChange,
  onTagsChange,
}: TagInputProps) {
  const inputId = useId()
  const listId = useId()

  function tryAdd(raw: string) {
    const result = commitPendingTag(tags, raw)
    if (result.error) {
      return
    }
    if (result.tags !== tags) {
      onTagsChange(result.tags)
    }
    onDraftChange(result.draft)
  }

  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Enter' || event.key === ',') {
      event.preventDefault()
      tryAdd(draft)
    }
  }

  function handleChange(value: string) {
    const result = appendTagsFromCommaInput(tags, value)
    if (result.tags !== tags) {
      onTagsChange(result.tags)
    }
    onDraftChange(result.draft)
  }

  return (
    <div className="space-y-2">
      <Label htmlFor={inputId}>Tags</Label>
      {tags.length > 0 ? (
        <ul
          id={listId}
          aria-label="Tags"
          className="flex flex-wrap gap-1.5"
        >
          {tags.map((tag) => (
            <li key={tag}>
              <Badge
                variant="outline"
                className="gap-1 rounded-md px-2.5 py-1.5 text-[0.6875rem] font-normal"
              >
                {tag}
                <button
                  type="button"
                  className="rounded-sm text-muted-foreground hover:text-foreground"
                  aria-label={`Remove tag ${tag}`}
                  disabled={disabled}
                  onClick={() =>
                    onTagsChange(tags.filter((item) => item !== tag))
                  }
                >
                  <X className="size-3.5" aria-hidden />
                </button>
              </Badge>
            </li>
          ))}
        </ul>
      ) : null}
      <div className="flex gap-2">
        <Input
          ref={inputRef}
          id={inputId}
          value={draft}
          disabled={disabled}
          aria-invalid={invalid || undefined}
          aria-describedby={errorId}
          aria-controls={listId}
          placeholder="Add a tag"
          className={cn(
            'h-12 rounded-[10px]',
            invalid && 'border-primary',
          )}
          onChange={(event) => handleChange(event.target.value)}
          onKeyDown={handleKeyDown}
        />
        <Button
          type="button"
          variant="outline"
          className="h-12 shrink-0 rounded-[10px] px-4"
          disabled={disabled}
          onClick={() => tryAdd(draft)}
        >
          Add
        </Button>
      </div>
      {errorMessage ? (
        <p id={errorId} className="text-[0.6875rem] text-primary" role="alert">
          {errorMessage}
        </p>
      ) : (
        <p className="text-[0.6875rem] text-muted-foreground">
          Add with Enter, comma, or Add. Up to {TAGS_MAX} tags,{' '}
          {TAG_MAX_LENGTH} characters each.
        </p>
      )}
    </div>
  )
}
