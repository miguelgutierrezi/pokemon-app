import type { RouteObject } from 'react-router'

import { AuthLayout } from '@/app/auth-layout'
import { AppLayout } from '@/app/layout'
import { NotFoundPage } from '@/app/not-found'
import { CatalogPage } from '@/pages/CatalogPage'
import { CollectionEditPage } from '@/pages/CollectionEditPage'
import { CollectionPage } from '@/pages/CollectionPage'
import { LoginPage } from '@/pages/LoginPage'
import { PokemonDetailPage } from '@/pages/PokemonDetailPage'
import { RegisterPage } from '@/pages/RegisterPage'

export const routerConfig: RouteObject[] = [
  {
    element: <AppLayout />,
    children: [
      { path: '/', element: <CatalogPage /> },
      { path: 'pokemon/:id', element: <PokemonDetailPage /> },
      { path: 'collection', element: <CollectionPage /> },
      { path: 'collection/:id', element: <CollectionEditPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
  {
    element: <AuthLayout />,
    children: [
      { path: 'login', element: <LoginPage /> },
      { path: 'register', element: <RegisterPage /> },
    ],
  },
]
