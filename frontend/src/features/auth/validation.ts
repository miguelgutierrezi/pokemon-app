export type RegisterFields = {
  name: string
  email: string
  password: string
}

export type FieldErrors = Partial<Record<keyof RegisterFields, string>>

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function utf8ByteLength(value: string): number {
  return new TextEncoder().encode(value).length
}

export function validateRegister(fields: RegisterFields): FieldErrors {
  const errors: FieldErrors = {}
  const name = fields.name.trim()
  if (name.length < 1 || name.length > 100) {
    errors.name = 'Name must be between 1 and 100 characters.'
  }

  const email = fields.email.trim()
  if (!EMAIL_PATTERN.test(email)) {
    errors.email = 'Enter a valid email address.'
  }

  const password = fields.password
  const bytes = utf8ByteLength(password)
  // Mirror backend @NotBlank: whitespace-only is invalid (same message).
  if (
    password.trim().length === 0 ||
    password.length < 8 ||
    bytes > 72
  ) {
    errors.password =
      'Password must be at least 8 characters and at most 72 UTF-8 bytes.'
  }

  return errors
}

export function firstInvalidField(
  errors: FieldErrors,
): keyof RegisterFields | null {
  if (errors.name) return 'name'
  if (errors.email) return 'email'
  if (errors.password) return 'password'
  return null
}
