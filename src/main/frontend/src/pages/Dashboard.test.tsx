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
    expect(client.fetchStocks).toHaveBeenCalledWith('brazil', expect.any(AbortSignal))
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
    expect(client.fetchStocks).toHaveBeenLastCalledWith('us', expect.any(AbortSignal))
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

  describe('keyboard navigation', () => {
    it('navigates between tabs with arrow keys', async () => {
      const user = userEvent.setup()
      render(<Dashboard />)

      await waitFor(() => {
        expect(screen.getByText('PETR4')).toBeInTheDocument()
      })

      const brazilTab = screen.getByRole('tab', { name: /brazil stocks/i })
      const usTab = screen.getByRole('tab', { name: /us stocks/i })

      // Focus the Brazil tab first
      brazilTab.focus()
      expect(brazilTab).toHaveFocus()

      // Navigate to US tab with arrow right
      await user.keyboard('{ArrowRight}')
      expect(usTab).toHaveFocus()

      await waitFor(() => {
        expect(screen.getByText('AAPL')).toBeInTheDocument()
      })

      // Navigate back to Brazil tab with arrow left
      await user.keyboard('{ArrowLeft}')
      expect(brazilTab).toHaveFocus()

      await waitFor(() => {
        expect(screen.getByText('PETR4')).toBeInTheDocument()
      })
    })

    it('wraps focus when navigating beyond tab boundaries', async () => {
      const user = userEvent.setup()
      render(<Dashboard />)

      await waitFor(() => {
        expect(screen.getByText('PETR4')).toBeInTheDocument()
      })

      const brazilTab = screen.getByRole('tab', { name: /brazil stocks/i })
      const usTab = screen.getByRole('tab', { name: /us stocks/i })

      // Start at Brazil tab
      brazilTab.focus()

      // Navigate left from first tab should wrap to last tab
      await user.keyboard('{ArrowLeft}')
      expect(usTab).toHaveFocus()

      // Navigate right from last tab should wrap to first tab
      await user.keyboard('{ArrowRight}')
      expect(brazilTab).toHaveFocus()
    })

    it('maintains proper tabIndex values for accessibility', () => {
      render(<Dashboard />)

      const brazilTab = screen.getByRole('tab', { name: /brazil stocks/i })
      const usTab = screen.getByRole('tab', { name: /us stocks/i })

      // Active tab should have tabIndex 0
      expect(brazilTab).toHaveAttribute('tabIndex', '0')
      // Inactive tab should have tabIndex -1
      expect(usTab).toHaveAttribute('tabIndex', '-1')
    })

    it('has proper ARIA attributes for accessibility', async () => {
      render(<Dashboard />)

      const brazilTab = screen.getByRole('tab', { name: /brazil stocks/i })
      const usTab = screen.getByRole('tab', { name: /us stocks/i })
      const tabPanel = screen.getByRole('tabpanel')

      // Wait for data to load
      await waitFor(() => {
        expect(screen.getByText('PETR4')).toBeInTheDocument()
      })

      // Tabs should have proper ARIA attributes
      expect(brazilTab).toHaveAttribute('aria-selected', 'true')
      expect(usTab).toHaveAttribute('aria-selected', 'false')
      expect(brazilTab).toHaveAttribute('aria-controls', 'stock-tabpanel')
      expect(usTab).toHaveAttribute('aria-controls', 'stock-tabpanel')
      expect(brazilTab).toHaveAttribute('id', 'brazil-tab')
      expect(usTab).toHaveAttribute('id', 'us-tab')

      // Tab panel should reference active tab
      expect(tabPanel).toHaveAttribute('aria-labelledby', 'brazil-tab')
      expect(tabPanel).toHaveAttribute('id', 'stock-tabpanel')
      expect(tabPanel).toHaveAttribute('tabIndex', '-1') // -1 when showing data table
    })

    it('sets tab panel tabIndex based on content state', () => {
      vi.mocked(client.fetchStocks).mockReturnValue(new Promise(() => {})) // Loading state
      render(<Dashboard />)

      const tabPanel = screen.getByRole('tabpanel')
      expect(tabPanel).toHaveAttribute('tabIndex', '0') // 0 during loading for keyboard access
    })
  })
})
