export type EditFieldErrors = {
  localizedName?: string
  location?: string
  tags?: string
}

export const LOCALIZED_NAME_MAX = 100
export const LOCATION_MAX = 200
export const TAGS_MAX = 20
export const TAG_MAX_LENGTH = 32

export function normalizeTag(raw: string): string | null {
  const trimmed = raw.trim().toLowerCase()
  if (trimmed.length === 0) {
    return null
  }
  return trimmed
}

/**
 * Commit a pending tag draft into the list (submit / Enter / Add).
 * Empty draft is a no-op. Invalid draft returns an error without changing tags.
 */
export function commitPendingTag(
  tags: string[],
  draft: string,
): { tags: string[]; draft: string; error?: string } {
  if (draft.trim().length === 0) {
    return { tags, draft: '' }
  }
  const next = normalizeTag(draft)
  if (next == null) {
    return { tags, draft: '' }
  }
  if (next.length > TAG_MAX_LENGTH) {
    return {
      tags,
      draft,
      error: `Each tag must be at most ${TAG_MAX_LENGTH} characters.`,
    }
  }
  if (tags.includes(next)) {
    return { tags, draft: '' }
  }
  if (tags.length >= TAGS_MAX) {
    return {
      tags,
      draft,
      error: `Use at most ${TAGS_MAX} tags.`,
    }
  }
  return { tags: [...tags, next], draft: '' }
}

/**
 * Split comma-separated input (paste / mobile Unidentified keys).
 * Complete parts before the last comma are added; the trailing fragment stays.
 */
export function appendTagsFromCommaInput(
  tags: string[],
  value: string,
): { tags: string[]; draft: string } {
  if (!value.includes(',')) {
    return { tags, draft: value }
  }
  const segments = value.split(',')
  const trailing = segments.pop() ?? ''
  const next = [...tags]
  for (const segment of segments) {
    const normalized = normalizeTag(segment)
    if (normalized == null) {
      continue
    }
    if (normalized.length > TAG_MAX_LENGTH) {
      continue
    }
    if (next.includes(normalized) || next.length >= TAGS_MAX) {
      continue
    }
    next.push(normalized)
  }
  return { tags: next, draft: trailing }
}

export function validateEditForm(input: {
  localizedName: string
  location: string
  tags: string[]
}): EditFieldErrors {
  const errors: EditFieldErrors = {}

  if (input.localizedName.length > LOCALIZED_NAME_MAX) {
    errors.localizedName = `Use at most ${LOCALIZED_NAME_MAX} characters.`
  }

  if (input.location.length > LOCATION_MAX) {
    errors.location = `Use at most ${LOCATION_MAX} characters.`
  }

  if (input.tags.length > TAGS_MAX) {
    errors.tags = `Use at most ${TAGS_MAX} tags.`
  } else {
    const overlong = input.tags.find((tag) => tag.length > TAG_MAX_LENGTH)
    if (overlong) {
      errors.tags = `Each tag must be at most ${TAG_MAX_LENGTH} characters.`
    }
  }

  return errors
}

export function firstInvalidEditField(
  errors: EditFieldErrors,
): keyof EditFieldErrors | null {
  if (errors.localizedName) {
    return 'localizedName'
  }
  if (errors.location) {
    return 'location'
  }
  if (errors.tags) {
    return 'tags'
  }
  return null
}

export function toUpdateBody(input: {
  localizedName: string
  location: string
  tags: string[]
  version: number
}) {
  const localized = input.localizedName.trim()
  const location = input.location.trim()
  return {
    localizedName: localized.length === 0 ? null : localized,
    location: location.length === 0 ? null : location,
    tags: input.tags,
    version: input.version,
  }
}
