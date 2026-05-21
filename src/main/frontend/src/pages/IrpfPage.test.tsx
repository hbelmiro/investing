import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { IrpfPage } from './IrpfPage'
import * as client from '../api/client'
import type { IrpfResponse } from '../api/types'

vi.mock('../api/client')

describe('IrpfPage', () => {
  const usData: IrpfResponse = [
    { symbol: 'AAPL', quantity: 12, avgCostUsd: 53.39, totalCostUsd: 640.68, avgCostBrl: 290.29, totalCostBrl: 3483.48, ptaxRate: 5.4369, capitalGainsBrl: 328.23, totalCapitalGainsBrl: 328.23, dividendsGrossBrl: 4.53, dividendsTaxBrl: 0.30 },
    { symbol: 'MSFT', quantity: 8, avgCostUsd: 40.03, totalCostUsd: 320.24, avgCostBrl: 241.63, totalCostBrl: 1933.04, ptaxRate: 6.037, capitalGainsBrl: 0, totalCapitalGainsBrl: 0, dividendsGrossBrl: 6.61, dividendsTaxBrl: 0 },
  ]

  const brData: IrpfResponse = [
    { symbol: 'PETR4', quantity: 120, avgCostBrl: 31.67, totalCostBrl: 3800.40, capitalGainsBrl: 249.84, totalCapitalGainsBrl: 249.84, dividendsGrossBrl: 1.50, dividendsTaxBrl: 0 },
    { symbol: 'MXRF11', quantity: 200, avgCostBrl: 10.50, totalCostBrl: 2100.00, capitalGainsBrl: 0, totalCapitalGainsBrl: 0, dividendsGrossBrl: 45.00, dividendsTaxBrl: 0 },
  ]

  const currentYear = new Date().getFullYear()

  beforeEach(() => {
    vi.mocked(client.fetchIrpfYears).mockResolvedValue([2020, 2021, 2022, 2023, 2024, currentYear])
    vi.mocked(client.fetchIrpfData).mockResolvedValue(usData)
    vi.mocked(client.fetchBrIrpfData).mockResolvedValue(brData)
  })

  it('renders year selector with current year selected by default', () => {
    render(<IrpfPage />)
    const select = screen.getByRole('combobox', { name: /ano/i })
    expect(select).toHaveValue(String(currentYear))
  })

  it('renders year options fetched from API', async () => {
    render(<IrpfPage />)

    await waitFor(() => {
      expect(screen.getByRole('option', { name: '2020' })).toBeInTheDocument()
    })

    const options = screen.getAllByRole('option')
    expect(options).toHaveLength(6)
    expect(screen.getByRole('option', { name: '2020' })).toBeInTheDocument()
    expect(screen.getByRole('option', { name: String(currentYear) })).toBeInTheDocument()
  })

  it('fetches both US and BR IRPF data on mount', async () => {
    render(<IrpfPage />)

    await waitFor(() => {
      expect(screen.getByText('AAPL')).toBeInTheDocument()
    })
    expect(client.fetchIrpfData).toHaveBeenCalledWith(currentYear, expect.any(AbortSignal))
    expect(client.fetchBrIrpfData).toHaveBeenCalledWith(currentYear, expect.any(AbortSignal))
  })

  it('displays both US and BR data after successful fetch', async () => {
    render(<IrpfPage />)

    await waitFor(() => {
      expect(screen.getByText('AAPL')).toBeInTheDocument()
      expect(screen.getByText('MSFT')).toBeInTheDocument()
      expect(screen.getByText('PETR4')).toBeInTheDocument()
      expect(screen.getByText('MXRF11')).toBeInTheDocument()
    })
  })

  it('renders section headings for US and BR tables', async () => {
    render(<IrpfPage />)

    await waitFor(() => {
      expect(screen.getByText('Ações US')).toBeInTheDocument()
      expect(screen.getByText('Ações BR e FIIs')).toBeInTheDocument()
    })
  })

  it('shows loading state while fetching', () => {
    vi.mocked(client.fetchIrpfData).mockReturnValue(new Promise(() => {}))
    vi.mocked(client.fetchBrIrpfData).mockReturnValue(new Promise(() => {}))
    render(<IrpfPage />)
    expect(screen.getByText(/carregando/i)).toBeInTheDocument()
  })

  it('shows error state on fetch failure', async () => {
    vi.mocked(client.fetchIrpfData).mockRejectedValue(new Error('Network error'))
    render(<IrpfPage />)

    await waitFor(() => {
      expect(screen.getByText(/erro/i)).toBeInTheDocument()
    })
  })

  it('fetches new data when year is changed', async () => {
    const user = userEvent.setup()
    render(<IrpfPage />)

    await waitFor(() => {
      expect(screen.getByText('AAPL')).toBeInTheDocument()
    })

    await waitFor(() => {
      expect(screen.getByRole('option', { name: '2024' })).toBeInTheDocument()
    })

    await user.selectOptions(screen.getByRole('combobox', { name: /ano/i }), '2024')

    await waitFor(() => {
      expect(client.fetchIrpfData).toHaveBeenCalledWith(2024, expect.any(AbortSignal))
      expect(client.fetchBrIrpfData).toHaveBeenCalledWith(2024, expect.any(AbortSignal))
    })
  })

  it('aborts previous request when year changes', async () => {
    const user = userEvent.setup()
    render(<IrpfPage />)

    await waitFor(() => {
      expect(screen.getByText('AAPL')).toBeInTheDocument()
    })

    const firstCallSignal = vi.mocked(client.fetchIrpfData).mock.calls[0][1] as AbortSignal

    await waitFor(() => {
      expect(screen.getByRole('option', { name: '2024' })).toBeInTheDocument()
    })

    await user.selectOptions(screen.getByRole('combobox', { name: /ano/i }), '2024')

    expect(firstCallSignal.aborted).toBe(true)
  })
})
