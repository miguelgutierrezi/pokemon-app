export type AuthUser = {
  id: number
  name: string
  email: string
}

export type LoginRequest = {
  email: string
  password: string
}

export type RegisterRequest = {
  name: string
  email: string
  password: string
}

export type AuthResponse = {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
  user: AuthUser
}

export type AbilitySummary = {
  name: string
  hidden: boolean
}

export type PokemonSummary = {
  nationalNumber: number
  name: string
  spriteUrl: string
  category: string
  weightKg: number
  abilities: AbilitySummary[]
}

export type PageEnvelope<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type PokemonPage = PageEnvelope<PokemonSummary>

export type PokemonStats = {
  hp: number
  attack: number
  defense: number
  specialAttack: number
  specialDefense: number
  speed: number
  total: number
}

export type FlavorText = {
  text: string
  version: string
}

export type EvolutionEdge = {
  trigger: string
  minLevel: number | null
  itemSlug: string | null
  node: EvolutionNode
}

export type EvolutionNode = {
  nationalNumber: number
  name: string
  imageUrl: string
  isCurrent: boolean
  children: EvolutionEdge[]
}

export type PokemonDetail = {
  nationalNumber: number
  name: string
  category: string
  types: string[]
  heightM: number
  weightKg: number
  abilities: AbilitySummary[]
  generation: string
  imageUrl: string
  stats: PokemonStats
  flavorText: FlavorText
  evolution: EvolutionNode
}

export type LocalPokemon = {
  nationalNumber: number
  name: string
  types: string[]
  heightM: number
  weightKg: number
  category: string
  generation: string
  imageUrl: string
  syncedAt: string
  localizedName: string | null
  location: string | null
  tags: string[]
  version: number
}

export type LocalPokemonPage = PageEnvelope<LocalPokemon>

export type ImportPokemonRequest = {
  nationalNumber: number
}

export type UpdateLocalPokemonRequest = {
  localizedName: string | null
  location: string | null
  tags: string[]
  version: number
}

export type PageQuery = {
  page: number
  size: number
}
