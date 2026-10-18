import { describe, expect, it } from 'vitest'

import {
  ApiError,
  extractField,
  genericHttpError,
  parseProblem,
} from '@/api/api-error'

describe('extractField', () => {
  it("extracts parameter 'password'", () => {
    expect(extractField("Invalid value for parameter 'password'")).toBe(
      'password',
    )
  })

  it("extracts property 'name' from unknown/read-only detail", () => {
    expect(
      extractField("Unknown or read-only property 'name'"),
    ).toBe('name')
  })

  it("extracts property 'version' from invalid/missing detail", () => {
    expect(
      extractField("Invalid or missing value for property 'version'"),
    ).toBe('version')
  })

  it('returns undefined when no quoted field', () => {
    expect(extractField('Something went wrong')).toBeUndefined()
  })

  it('prefers parameter over property when both present', () => {
    expect(
      extractField("Invalid value for parameter 'email' and property 'name'"),
    ).toBe('email')
  })
})

describe('parseProblem', () => {
  it('maps problem+json body into ApiError with field', () => {
    const error = parseProblem(400, {
      type: 'urn:problem-type:bad-request',
      title: 'Bad Request',
      detail: "Invalid value for parameter 'password'",
      status: 400,
    })

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(400)
    expect(error.type).toBe('urn:problem-type:bad-request')
    expect(error.title).toBe('Bad Request')
    expect(error.detail).toBe("Invalid value for parameter 'password'")
    expect(error.field).toBe('password')
  })

  it('maps 401 unauthorized without field', () => {
    const error = parseProblem(401, {
      type: 'urn:problem-type:unauthorized',
      title: 'Unauthorized',
      detail: 'Invalid email or password',
      status: 401,
    })

    expect(error.status).toBe(401)
    expect(error.type).toBe('urn:problem-type:unauthorized')
    expect(error.field).toBeUndefined()
  })

  it('maps 404 not-found', () => {
    const error = parseProblem(404, {
      type: 'urn:problem-type:not-found',
      title: 'Not Found',
      detail: 'Pokémon not found',
      status: 404,
    })

    expect(error.status).toBe(404)
    expect(error.type).toBe('urn:problem-type:not-found')
  })

  it('distinguishes 409 conflict vs stale-version by type', () => {
    const conflict = parseProblem(409, {
      type: 'urn:problem-type:conflict',
      title: 'Conflict',
      detail: 'Already in local collection',
      status: 409,
    })
    const stale = parseProblem(409, {
      type: 'urn:problem-type:stale-version',
      title: 'Conflict',
      detail: 'Modified elsewhere',
      status: 409,
    })

    expect(conflict.type).toBe('urn:problem-type:conflict')
    expect(stale.type).toBe('urn:problem-type:stale-version')
  })

  it('maps 502 bad-gateway', () => {
    const error = parseProblem(502, {
      type: 'urn:problem-type:bad-gateway',
      title: 'Bad Gateway',
      detail: 'Upstream temporarily unavailable',
      status: 502,
    })

    expect(error.status).toBe(502)
    expect(error.type).toBe('urn:problem-type:bad-gateway')
  })
})

describe('genericHttpError', () => {
  it('never includes response body in detail', () => {
    const error = genericHttpError(502)

    expect(error.status).toBe(502)
    expect(error.detail).not.toMatch(/<html/i)
    expect(error.detail).toMatch(/try again|unexpected|error/i)
  })
})
