export class ApiError extends Error {
  readonly status: number
  readonly type: string
  readonly title: string
  readonly detail: string
  readonly field?: string

  constructor(init: {
    status: number
    type: string
    title: string
    detail: string
    field?: string
  }) {
    super(init.detail)
    this.name = 'ApiError'
    this.status = init.status
    this.type = init.type
    this.title = init.title
    this.detail = init.detail
    this.field = init.field
  }
}

const PARAMETER_FIELD = /parameter '([^']+)'/
const PROPERTY_FIELD = /property '([^']+)'/

export function extractField(detail: string): string | undefined {
  const parameter = PARAMETER_FIELD.exec(detail)
  if (parameter?.[1]) {
    return parameter[1]
  }
  const property = PROPERTY_FIELD.exec(detail)
  if (property?.[1]) {
    return property[1]
  }
  return undefined
}

type ProblemBody = {
  type?: unknown
  title?: unknown
  detail?: unknown
  status?: unknown
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

export function parseProblem(status: number, body: unknown): ApiError {
  const problem = isRecord(body) ? (body as ProblemBody) : {}
  const detail =
    typeof problem.detail === 'string' && problem.detail.length > 0
      ? problem.detail
      : 'Unexpected error. Try again.'
  const type = typeof problem.type === 'string' ? problem.type : ''
  const title =
    typeof problem.title === 'string' && problem.title.length > 0
      ? problem.title
      : 'Error'

  return new ApiError({
    status,
    type,
    title,
    detail,
    field: extractField(detail),
  })
}

export function networkError(): ApiError {
  return new ApiError({
    status: 0,
    type: 'network',
    title: 'Network Error',
    detail: 'Network error. Try again.',
  })
}

export function genericHttpError(status: number): ApiError {
  return new ApiError({
    status,
    type: '',
    title: 'Error',
    detail: 'Unexpected error. Try again.',
  })
}
