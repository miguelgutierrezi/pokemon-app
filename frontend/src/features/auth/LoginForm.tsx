import { ArrowRight, Eye, EyeOff, HardDrive, LockKeyhole, Mail } from 'lucide-react'
import { useId, useRef, useState, type FormEvent } from 'react'
import { Link } from 'react-router'

import { ApiError } from '@/api/api-error'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { authLinkWithRedirect } from '@/features/auth/auth-redirect'
import { useLogin } from '@/features/auth/useLogin'
import { useAuth } from '@/features/auth/useAuth'
import { cn } from '@/lib/utils'

export type LoginFormProps = {
  redirect: string | null
  sessionExpired: boolean
  onSuccess: (redirect: string | null) => void
}

export function LoginForm({
  redirect,
  sessionExpired,
  onSuccess,
}: LoginFormProps) {
  const { signIn } = useAuth()
  const loginMutation = useLogin()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [authError, setAuthError] = useState<string | null>(null)
  const alertRef = useRef<HTMLDivElement>(null)
  const emailRef = useRef<HTMLInputElement>(null)
  const demoId = useId()
  const errorId = useId()

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setAuthError(null)
    try {
      const response = await loginMutation.mutateAsync({ email, password })
      signIn(response)
      onSuccess(redirect)
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        setAuthError('Invalid email or password')
        queueMicrotask(() => {
          alertRef.current?.focus()
          if (document.activeElement !== alertRef.current) {
            emailRef.current?.focus()
          }
        })
        return
      }
      setAuthError(
        error instanceof ApiError
          ? error.detail
          : 'Unexpected error. Try again.',
      )
      queueMicrotask(() => alertRef.current?.focus())
    }
  }

  return (
    <div className="flex w-full max-w-md flex-col gap-8">
      <div className="space-y-4">
        <div className="inline-flex items-center gap-1.5 rounded-sm bg-danger-muted px-2.5 py-1.5">
          <span className="size-3.5 rounded-full bg-primary/20" aria-hidden />
          <p className="text-caption font-semibold text-primary uppercase">
            Trainer access
          </p>
        </div>
        <h1
          tabIndex={-1}
          className="text-[1.75rem] leading-tight font-semibold tracking-tight text-foreground outline-none md:text-[2.125rem]"
        >
          Welcome back
        </h1>
        <p className="text-[0.9375rem] leading-relaxed text-muted-foreground">
          Sign in to add Pokémon to the shared collection and edit their
          details.
        </p>
      </div>

      {sessionExpired ? (
        <Alert className="rounded-md border-danger-border bg-danger-muted">
          <AlertTitle>Your session expired</AlertTitle>
          <AlertDescription>
            Sign in again to continue where you left off.
          </AlertDescription>
        </Alert>
      ) : null}

      {authError ? (
        <Alert
          ref={alertRef}
          tabIndex={-1}
          className="rounded-md outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50"
          variant="destructive"
          id={errorId}
        >
          <AlertTitle>Sign in failed</AlertTitle>
          <AlertDescription>{authError}</AlertDescription>
        </Alert>
      ) : null}

      <form className="flex flex-col gap-5" onSubmit={(e) => void handleSubmit(e)} noValidate>
        <div className="space-y-2.5">
          <Label htmlFor="login-email" className="text-body font-medium">
            Email address
          </Label>
          <div className="relative">
            <Mail
              className="pointer-events-none absolute top-1/2 left-4 size-[1.1875rem] -translate-y-1/2 text-muted-foreground"
              aria-hidden
            />
            <Input
              ref={emailRef}
              id="login-email"
              name="email"
              type="email"
              autoComplete="email"
              placeholder="trainer@example.com"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              className="h-14 rounded-md bg-surface pl-12 text-[0.9375rem]"
              aria-describedby={authError ? errorId : undefined}
            />
          </div>
        </div>

        <div className="space-y-2.5">
          <Label htmlFor="login-password" className="text-body font-medium">
            Password
          </Label>
          <div className="relative">
            <LockKeyhole
              className="pointer-events-none absolute top-1/2 left-4 size-[1.1875rem] -translate-y-1/2 text-muted-foreground"
              aria-hidden
            />
            <Input
              id="login-password"
              name="password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="current-password"
              placeholder="Enter your password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="h-14 rounded-md bg-surface pr-12 pl-12 text-[0.9375rem]"
            />
            <button
              type="button"
              className="absolute top-1/2 right-2 flex size-10 -translate-y-1/2 items-center justify-center text-muted-foreground"
              onClick={() => setShowPassword((value) => !value)}
              aria-label={showPassword ? 'Hide password' : 'Show password'}
            >
              {showPassword ? (
                <EyeOff className="size-5" aria-hidden />
              ) : (
                <Eye className="size-5" aria-hidden />
              )}
            </button>
          </div>
        </div>

        <p
          id={demoId}
          className="rounded-md border border-border bg-surface px-3 py-2.5 font-mono text-caption text-muted-foreground"
        >
          <span className="font-sans font-medium text-foreground">
            Demo credential:{' '}
          </span>
          demo@pokefolio.dev / Demo-Passw0rd!
        </p>

        <Button
          type="submit"
          className="h-14 w-full rounded-md text-[0.9375rem] font-semibold"
          disabled={loginMutation.isPending}
        >
          {loginMutation.isPending ? 'Signing in…' : 'Sign in'}
          {!loginMutation.isPending ? (
            <ArrowRight className="size-[1.125rem]" aria-hidden />
          ) : null}
        </Button>
      </form>

      <p className="text-center text-body text-muted-foreground">
        Don&apos;t have an account?{' '}
        <Link
          to={authLinkWithRedirect('/register', redirect)}
          className="font-semibold text-primary underline underline-offset-2"
        >
          Create account
        </Link>
      </p>

      <div className="space-y-3 border-t border-border pt-5">
        <div className="flex gap-3">
          <HardDrive className="mt-0.5 size-[1.125rem] shrink-0 text-muted-foreground" aria-hidden />
          <div className="space-y-1 text-caption text-muted-foreground">
            <p className="font-medium text-muted-foreground">
              Shared collection on the server
            </p>
            <p>
              Anyone can browse the shared collection. Sign in to add Pokémon or
              edit their details.
            </p>
          </div>
        </div>
        <Link
          to="/"
          className={cn(
            'inline-flex h-12 w-full items-center justify-center gap-2 rounded-md border border-border bg-surface text-body font-semibold text-primary',
            'hover:bg-muted md:h-auto md:w-auto md:justify-start md:border-0 md:bg-transparent md:underline md:underline-offset-2',
          )}
        >
          Browse without an account
        </Link>
      </div>
    </div>
  )
}
