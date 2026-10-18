import type { ApiClient } from '@/api/client'
import type {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
} from '@/api/types'

export function login(
  client: ApiClient,
  body: LoginRequest,
  signal?: AbortSignal,
): Promise<AuthResponse> {
  return client.request<AuthResponse>('/api/v1/auth/login', {
    method: 'POST',
    body,
    signal,
  })
}

export function register(
  client: ApiClient,
  body: RegisterRequest,
  signal?: AbortSignal,
): Promise<AuthResponse> {
  return client.request<AuthResponse>('/api/v1/auth/register', {
    method: 'POST',
    body,
    signal,
  })
}
