import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { Navbar } from './Navbar'

describe('Navbar', () => {
  it('renders the application title', () => {
    render(
      <MemoryRouter>
        <Navbar />
      </MemoryRouter>
    )
    expect(screen.getByText('Investing')).toBeInTheDocument()
  })

  it('renders IRPF navigation link', () => {
    render(
      <MemoryRouter>
        <Navbar />
      </MemoryRouter>
    )
    const link = screen.getByRole('link', { name: /irpf/i })
    expect(link).toBeInTheDocument()
    expect(link).toHaveAttribute('href', '/irpf')
  })
})
