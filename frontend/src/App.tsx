import { Button } from '@/components/ui/button'

function App() {
  return (
    <main className="mx-auto flex min-h-screen max-w-2xl flex-col justify-center gap-4 bg-background px-6">
      <p className="font-mono text-[10px] tracking-wide text-primary uppercase">
        National Pokédex
      </p>
      <h1 className="text-display font-semibold tracking-tight text-foreground">
        Pokéfolio
      </h1>
      <p className="text-body text-muted-foreground">
        Setup complete — features coming soon.
      </p>
      <div>
        <Button type="button">Browse catalog</Button>
      </div>
    </main>
  )
}

export default App
