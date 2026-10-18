import { useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router'

import { resolvePostAuthPath } from '@/features/auth/auth-redirect'
import { LoginForm } from '@/features/auth/LoginForm'
import { useAuth } from '@/features/auth/useAuth'

export function LoginPage() {
  const { isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const redirect = searchParams.get('redirect')
  const sessionExpired = searchParams.get('reason') === 'expired'
  const destination = resolvePostAuthPath(redirect)

  useEffect(() => {
    if (isAuthenticated) {
      void navigate(destination, { replace: true })
    }
  }, [destination, isAuthenticated, navigate])

  if (isAuthenticated) {
    return null
  }

  return (
    <LoginForm
      redirect={redirect}
      sessionExpired={sessionExpired}
      onSuccess={(next) => {
        void navigate(resolvePostAuthPath(next), { replace: true })
      }}
    />
  )
}
