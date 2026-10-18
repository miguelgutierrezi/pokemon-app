import { useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router'

import { resolvePostAuthPath } from '@/features/auth/auth-redirect'
import { RegisterForm } from '@/features/auth/RegisterForm'
import { useAuth } from '@/features/auth/useAuth'

export function RegisterPage() {
  const { isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const redirect = searchParams.get('redirect')
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
    <RegisterForm
      redirect={redirect}
      onSuccess={(next) => {
        void navigate(resolvePostAuthPath(next), { replace: true })
      }}
    />
  )
}
