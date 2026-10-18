import { genericHttpError, networkError, parseProblem } from '@/api/api-error'

export type ApiClientOptions = {
  getToken: () => string | null | undefined
}

export type RequestOptions = {
  method?: string
  body?: unknown
  auth?: boolean
  signal?: AbortSignal
  headers?: HeadersInit
}

export type ApiClient = {
  request: <T>(path: string, options?: RequestOptions) => Promise<T>
}

function isProblemContentType(contentType: string | null): boolean {
  return Boolean(contentType?.includes('application/problem+json'))
}

function isJsonContentType(contentType: string | null): boolean {
  return Boolean(
    contentType?.includes('application/json') ||
      contentType?.includes('application/problem+json'),
  )
}

export function createApiClient({ getToken }: ApiClientOptions): ApiClient {
  async function request<T>(
    path: string,
    options: RequestOptions = {},
  ): Promise<T> {
    const headers = new Headers(options.headers)
    if (options.body !== undefined && !headers.has('Content-Type')) {
      headers.set('Content-Type', 'application/json')
    }
    if (options.auth) {
      const token = getToken()
      if (token) {
        headers.set('Authorization', `Bearer ${token}`)
      }
    }

    let response: Response
    try {
      response = await fetch(path, {
        method: options.method ?? 'GET',
        headers,
        body:
          options.body === undefined ? undefined : JSON.stringify(options.body),
        signal: options.signal,
      })
    } catch (cause) {
      if (
        options.signal?.aborted ||
        (cause instanceof Error && cause.name === 'AbortError')
      ) {
        throw cause
      }
      throw networkError()
    }

    if (response.status === 204) {
      return undefined as T
    }

    const contentType = response.headers.get('Content-Type')
    const rawText = await response.text()
    let parsed: unknown
    if (rawText.length > 0 && isJsonContentType(contentType)) {
      try {
        parsed = JSON.parse(rawText) as unknown
      } catch {
        parsed = undefined
      }
    }

    if (!response.ok) {
      if (isProblemContentType(contentType) && parsed !== undefined) {
        throw parseProblem(response.status, parsed)
      }
      throw genericHttpError(response.status)
    }

    if (rawText.length === 0) {
      return undefined as T
    }

    if (parsed === undefined) {
      throw genericHttpError(response.status)
    }

    return parsed as T
  }

  return { request }
}
