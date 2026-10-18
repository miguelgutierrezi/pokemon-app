import { describe, expect, it } from 'vitest'

import { sanitizeReturnPath } from '@/lib/return-path'

describe('sanitizeReturnPath', () => {
  it('keeps safe internal paths including query', () => {
    expect(sanitizeReturnPath('/')).toBe('/')
    expect(sanitizeReturnPath('/pokemon/1')).toBe('/pokemon/1')
    expect(sanitizeReturnPath('/collection?page=2')).toBe('/collection?page=2')
    expect(sanitizeReturnPath('/x?y')).toBe('/x?y')
  })

  it('rejects null, empty, and whitespace → /', () => {
    expect(sanitizeReturnPath(null)).toBe('/')
    expect(sanitizeReturnPath(undefined)).toBe('/')
    expect(sanitizeReturnPath('')).toBe('/')
    expect(sanitizeReturnPath('   ')).toBe('/')
  })

  it('rejects absolute and dangerous schemes', () => {
    expect(sanitizeReturnPath('http://evil.example')).toBe('/')
    expect(sanitizeReturnPath('https://evil.example/path')).toBe('/')
    expect(sanitizeReturnPath('javascript:alert(1)')).toBe('/')
    expect(sanitizeReturnPath('data:text/html,hi')).toBe('/')
  })

  it('rejects protocol-relative and backslash tricks', () => {
    expect(sanitizeReturnPath('//evil')).toBe('/')
    expect(sanitizeReturnPath('/\\evil')).toBe('/')
    expect(sanitizeReturnPath('\\\\evil')).toBe('/')
  })

  it('rejects encoded variants after one decodeURIComponent', () => {
    expect(sanitizeReturnPath('/%2F%2Fevil')).toBe('/')
    expect(sanitizeReturnPath('/%5C%5Cevil')).toBe('/')
    expect(sanitizeReturnPath('%2F%2Fevil')).toBe('/')
    expect(sanitizeReturnPath('/%2f%2fevil')).toBe('/')
  })
})
