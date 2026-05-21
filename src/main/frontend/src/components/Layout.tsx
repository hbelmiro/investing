import type { ReactNode } from 'react'
import { Navbar } from './Navbar'

interface LayoutProps {
  readonly children: ReactNode
}

export function Layout({ children }: LayoutProps) {
  return (
    <>
      <Navbar />
      <main className="main-content">{children}</main>
    </>
  )
}
