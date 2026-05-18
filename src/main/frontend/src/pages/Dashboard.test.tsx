import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Dashboard } from './Dashboard'
import * as client from '../api/client'
import type { StocksResponse } from '../api/types'

vi.mock('../api/client')

describe('Dashboard', () => {
  const brazilData: StocksResponse = [
    ['PETR4', '1.234', 'R$ 100,00', 'R$ 120,00', '20,00%', 'R$ 50,00'],
  ]

  const usData: StocksResponse = [
    ['AAPL', '10', 'US$ 150,00', 'US$ 170,00', '13,33%', 'US$ 5,00'],
  ]

  beforeEach(() => {
    vi.mocked(client.fetchStocks).mockImplementation(async (market) => {
      return market === 'brazil' ? brazilData : usData
    })
  })

  it('renders Brazil stocks tab by default and loads data', async () => {
    render(<Dashboard />)

    await waitFor(() => {
      expect(screen.getByText('PETR4')).toBeInTheDocument()
    })
    expect(client.fetchStocks).toHaveBeenCalledWith('brazil')
  })

  it('switches to US stocks tab and loads data', async () => {
    const user = userEvent.setup()
    render(<Dashboard />)

    await waitFor(() => {
      expect(screen.getByText('PETR4')).toBeInTheDocument()
    })

    await user.click(screen.getByRole('tab', { name: /us stocks/i }))

    await waitFor(() => {
      expect(screen.getByText('AAPL')).toBeInTheDocument()
    })
    expect(client.fetchStocks).toHaveBeenCalledWith('us')
  })

  it('shows loading state while fetching', () => {
    vi.mocked(client.fetchStocks).mockReturnValue(new Promise(() => {}))
    render(<Dashboard />)
    expect(screen.getByText(/carregando/i)).toBeInTheDocument()
  })

  it('shows error state on fetch failure', async () => {
    vi.mocked(client.fetchStocks).mockRejectedValue(new Error('Network error'))
    render(<Dashboard />)

    await waitFor(() => {
      expect(screen.getByText(/erro/i)).toBeInTheDocument()
    })
  })
})
