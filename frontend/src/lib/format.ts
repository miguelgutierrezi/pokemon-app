const ROMAN: Record<string, string> = {
  '1': 'I',
  '2': 'II',
  '3': 'III',
  '4': 'IV',
  '5': 'V',
  '6': 'VI',
  '7': 'VII',
  '8': 'VIII',
  '9': 'IX',
  i: 'I',
  ii: 'II',
  iii: 'III',
  iv: 'IV',
  v: 'V',
  vi: 'VI',
  vii: 'VII',
  viii: 'VIII',
  ix: 'IX',
}

export function formatSlug(slug: string): string {
  return slug
    .split('-')
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
    .join(' ')
}

export function formatNationalNumber(nationalNumber: number): string {
  return `#${String(nationalNumber).padStart(4, '0')}`
}

export function formatWeightKg(weightKg: number): string {
  return `${weightKg.toFixed(1)} kg`
}

export function formatHeightM(heightM: number): string {
  return `${heightM.toFixed(1)} m`
}

export function formatGeneration(generation: string): string {
  const match = /^generation[-_]?(.+)$/i.exec(generation.trim())
  if (!match?.[1]) {
    return formatSlug(generation)
  }
  const token = match[1].toLowerCase()
  const roman = ROMAN[token] ?? ROMAN[String(Number(token))] ?? token.toUpperCase()
  return `Generation ${roman}`
}

/** Inclusive national-number range for the current page, e.g. "0001–0009 of 1,025". */
export function formatRecordRange(args: {
  page: number
  size: number
  totalElements: number
  contentLength: number
}): string {
  if (args.totalElements === 0 || args.contentLength === 0) {
    return `0000–0000 of ${args.totalElements.toLocaleString()}`
  }
  const start = (args.page - 1) * args.size + 1
  const end = start + args.contentLength - 1
  const startLabel = formatNationalNumber(start).slice(1)
  const endLabel = formatNationalNumber(end).slice(1)
  return `${startLabel}–${endLabel} of ${args.totalElements.toLocaleString()}`
}

export function formatFlavorSource(version: string): string {
  return `Pokémon ${formatSlug(version)} · via PokéAPI`
}

export function formatEvolutionEdgeLabel(edge: {
  trigger: string
  minLevel: number | null
  itemSlug: string | null
}): string {
  if (edge.minLevel != null) {
    return `Lv. ${edge.minLevel}`
  }
  if (edge.itemSlug) {
    return formatSlug(edge.itemSlug)
  }
  return formatSlug(edge.trigger)
}

export function formatOptionalText(value: string | null | undefined): string {
  if (value == null || value.trim() === '') {
    return '—'
  }
  return value
}
