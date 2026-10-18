import { describe, expect, it } from 'vitest'

import {
  formatEvolutionEdgeLabel,
  formatFlavorSource,
  formatGeneration,
  formatHeightM,
  formatNationalNumber,
  formatOptionalText,
  formatSlug,
  formatWeightKg,
} from '@/lib/format'

describe('formatSlug', () => {
  it('title-cases single and hyphenated slugs', () => {
    expect(formatSlug('overgrow')).toBe('Overgrow')
    expect(formatSlug('solar-power')).toBe('Solar Power')
  })
})

describe('formatNationalNumber', () => {
  it('pads to four digits with a hash prefix', () => {
    expect(formatNationalNumber(1)).toBe('#0001')
  })
})

describe('formatWeightKg', () => {
  it('formats with one decimal and kg unit', () => {
    expect(formatWeightKg(6.9)).toBe('6.9 kg')
    expect(formatWeightKg(60)).toBe('60.0 kg')
  })
})

describe('formatHeightM', () => {
  it('formats with one decimal and m unit', () => {
    expect(formatHeightM(0.7)).toBe('0.7 m')
  })
})

describe('formatGeneration', () => {
  it('maps generation slugs to Generation + Roman numeral', () => {
    expect(formatGeneration('generation-iv')).toBe('Generation IV')
  })
})

describe('formatFlavorSource', () => {
  it('prefixes the formatted version slug', () => {
    expect(formatFlavorSource('red')).toBe('Pokémon Red · via PokéAPI')
  })
})

describe('formatEvolutionEdgeLabel', () => {
  it('prefers Lv. when minLevel is set', () => {
    expect(
      formatEvolutionEdgeLabel({
        trigger: 'level-up',
        minLevel: 16,
        itemSlug: null,
      }),
    ).toBe('Lv. 16')
  })

  it('falls back to item then trigger when minLevel is null', () => {
    expect(
      formatEvolutionEdgeLabel({
        trigger: 'use-item',
        minLevel: null,
        itemSlug: 'water-stone',
      }),
    ).toBe('Water Stone')
  })
})

describe('formatOptionalText', () => {
  it('shows an em dash for empty values', () => {
    expect(formatOptionalText(null)).toBe('—')
    expect(formatOptionalText('')).toBe('—')
    expect(formatOptionalText('Bulbizarre')).toBe('Bulbizarre')
  })
})
