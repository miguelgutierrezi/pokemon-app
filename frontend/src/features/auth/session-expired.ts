type Listener = () => void

let open = false
const listeners = new Set<Listener>()

function notify(): void {
  for (const listener of listeners) {
    listener()
  }
}

export function getSessionExpiredOpen(): boolean {
  return open
}

export function openSessionExpired(): void {
  if (open) {
    return
  }
  open = true
  notify()
}

export function closeSessionExpired(): void {
  if (!open) {
    return
  }
  open = false
  notify()
}

/** Test helper — resets module state between cases. */
export function resetSessionExpiredState(): void {
  open = false
}

export function subscribeSessionExpired(listener: Listener): () => void {
  listeners.add(listener)
  return () => {
    listeners.delete(listener)
  }
}
