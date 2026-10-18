import { ArrowRight, Eye, EyeOff, HardDrive, LockKeyhole, Mail, UserRound } from 'lucide-react'
import { useId, useRef, useState, type FormEvent } from 'react'
import { Link } from 'react-router'

import { ApiError } from '@/api/api-error'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { authLinkWithRedirect } from '@/features/auth/auth-redirect'
import { useAuth } from '@/features/auth/useAuth'
import { useRegister } from '@/features/auth/useRegister'
import {
  firstInvalidField,
  validateRegister,
  type FieldErrors,
} from '@/features/auth/validation'
import { cn } from '@/lib/utils'

export type RegisterFormProps = {
  redirect: string | null
  onSuccess: (redirect: string | null) => void
}

export function RegisterForm({ redirect, onSuccess }: RegisterFormProps) {
  const { signIn } = useAuth()
  const registerMutation = useRegister()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({})
  const nameRef = useRef<HTMLInputElement>(null)
  const emailRef = useRef<HTMLInputElement>(null)
  const passwordRef = useRef<HTMLInputElement>(null)
  const nameErrorId = useId()
  const emailErrorId = useId()
  const passwordErrorId = useId()

  function focusField(field: keyof FieldErrors) {
    const map = {
      name: nameRef,
      email: emailRef,
      password: passwordRef,
    } as const
    map[field].current?.focus()
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const clientErrors = validateRegister({ name, email, password })
    if (Object.keys(clientErrors).length > 0) {
      setFieldErrors(clientErrors)
      const first = firstInvalidField(clientErrors)
      if (first) {
        queueMicrotask(() => focusField(first))
      }
      return
    }

    setFieldErrors({})
    try {
      const response = await registerMutation.mutateAsync({
        name: name.trim(),
        email: email.trim(),
        password,
      })
      signIn(response)
      onSuccess(redirect)
    } catch (error) {
      if (!(error instanceof ApiError)) {
        setFieldErrors({
          email: 'Unexpected error. Try again.',
        })
        queueMicrotask(() => focusField('email'))
        return
      }

      if (error.status === 409) {
        setFieldErrors({
          email: 'An account with this email already exists',
        })
        queueMicrotask(() => focusField('email'))
        return
      }

      if (error.status === 400 && error.field) {
        const field = error.field as keyof FieldErrors
        if (field === 'name' || field === 'email' || field === 'password') {
          setFieldErrors({ [field]: error.detail })
          queueMicrotask(() => focusField(field))
          return
        }
      }

      setFieldErrors({
        email: error.detail,
      })
      queueMicrotask(() => focusField('email'))
    }
  }

  return (
    <div className="flex w-full max-w-md flex-col gap-8">
      <div className="space-y-3">
        <div className="inline-flex items-center gap-1.5 rounded-sm bg-danger-muted px-2.5 py-1.5">
          <span className="size-3.5 rounded-full bg-primary/20" aria-hidden />
          <p className="text-caption font-semibold text-primary uppercase">
            Trainer access
          </p>
        </div>
        <h1
          tabIndex={-1}
          className="text-[1.75rem] leading-tight font-semibold tracking-tight text-foreground outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50 md:text-[2.125rem]"
        >
          Create an account
        </h1>
        <p className="text-[0.9375rem] leading-relaxed text-muted-foreground">
          Create an account to add Pokémon to the shared collection and edit
          their details.
        </p>
      </div>

      <form
        className="flex flex-col gap-5"
        onSubmit={(e) => void handleSubmit(e)}
        noValidate
      >
        <div className="space-y-2">
          <Label htmlFor="register-name" className="text-body font-medium">
            Name
          </Label>
          <div className="relative">
            <UserRound
              className="pointer-events-none absolute top-1/2 left-4 size-[1.1875rem] -translate-y-1/2 text-muted-foreground"
              aria-hidden
            />
            <Input
              ref={nameRef}
              id="register-name"
              name="name"
              autoComplete="name"
              placeholder="Your name"
              value={name}
              onChange={(event) => setName(event.target.value)}
              className="h-14 rounded-md bg-surface pl-12 text-[0.9375rem]"
              aria-invalid={fieldErrors.name ? true : undefined}
              aria-describedby={
                fieldErrors.name ? nameErrorId : undefined
              }
            />
          </div>
          {fieldErrors.name ? (
            <p id={nameErrorId} className="text-caption text-destructive">
              {fieldErrors.name}
            </p>
          ) : null}
        </div>

        <div className="space-y-2">
          <Label htmlFor="register-email" className="text-body font-medium">
            Email address
          </Label>
          <div className="relative">
            <Mail
              className="pointer-events-none absolute top-1/2 left-4 size-[1.1875rem] -translate-y-1/2 text-muted-foreground"
              aria-hidden
            />
            <Input
              ref={emailRef}
              id="register-email"
              name="email"
              type="email"
              autoComplete="email"
              placeholder="trainer@example.com"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              className="h-14 rounded-md bg-surface pl-12 text-[0.9375rem]"
              aria-invalid={fieldErrors.email ? true : undefined}
              aria-describedby={
                fieldErrors.email ? emailErrorId : undefined
              }
            />
          </div>
          {fieldErrors.email ? (
            <p id={emailErrorId} className="text-caption text-destructive">
              {fieldErrors.email}
            </p>
          ) : null}
        </div>

        <div className="space-y-2">
          <Label htmlFor="register-password" className="text-body font-medium">
            Password
          </Label>
          <div className="relative">
            <LockKeyhole
              className="pointer-events-none absolute top-1/2 left-4 size-[1.1875rem] -translate-y-1/2 text-muted-foreground"
              aria-hidden
            />
            <Input
              ref={passwordRef}
              id="register-password"
              name="password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="new-password"
              placeholder="Create a password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="h-14 rounded-md bg-surface pr-12 pl-12 text-[0.9375rem]"
              aria-invalid={fieldErrors.password ? true : undefined}
              aria-describedby={
                fieldErrors.password ? passwordErrorId : undefined
              }
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
          {fieldErrors.password ? (
            <p id={passwordErrorId} className="text-caption text-destructive">
              {fieldErrors.password}
            </p>
          ) : null}
        </div>

        <Button
          type="submit"
          className="h-14 w-full rounded-md text-[0.9375rem] font-semibold"
          disabled={registerMutation.isPending}
        >
          {registerMutation.isPending ? 'Creating account…' : 'Create account'}
          {!registerMutation.isPending ? (
            <ArrowRight className="size-[1.125rem]" aria-hidden />
          ) : null}
        </Button>
      </form>

      <p className="text-center text-body text-muted-foreground">
        Already have an account?{' '}
        <Link
          to={authLinkWithRedirect('/login', redirect)}
          className="font-semibold text-primary underline underline-offset-2"
        >
          Sign in
        </Link>
      </p>

      <div className="space-y-3 border-t border-border pt-5">
        <div className="flex gap-3">
          <HardDrive
            className="mt-0.5 size-[1.125rem] shrink-0 text-muted-foreground"
            aria-hidden
          />
          <div className="space-y-1 text-caption text-muted-foreground">
            <p className="font-medium">Shared collection on the server</p>
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
