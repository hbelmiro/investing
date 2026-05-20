import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { IrpfTable } from './IrpfTable'
import type { IrpfResponse } from '../api/types'

describe('IrpfTable', () => {
  const sampleData: IrpfResponse = [
    { symbol: 'AAPL', quantity: 12, avgCostUsd: 53.39, totalCostUsd: 640.68, avgCostBrl: 290.29, totalCostBrl: 3483.48, ptaxRate: 5.4369, capitalGainsBrl: 328.23, totalCapitalGainsBrl: 328.23, dividendsGrossBrl: 4.53, dividendsTaxBrl: 0.30 },
    { symbol: 'MSFT', quantity: 8, avgCostUsd: 40.03, totalCostUsd: 320.24, avgCostBrl: 241.63, totalCostBrl: 1933.04, ptaxRate: 6.037, capitalGainsBrl: 0, totalCapitalGainsBrl: 0, dividendsGrossBrl: 6.61, dividendsTaxBrl: 0 },
  ]

  it('renders all column headers', () => {
    render(<IrpfTable data={sampleData} />)
    expect(screen.getByText('Ativo')).toBeInTheDocument()
    expect(screen.getByText('Quantidade')).toBeInTheDocument()
    expect(screen.getByText('Custo Médio (USD)')).toBeInTheDocument()
    expect(screen.getByText('Custo Total (USD)')).toBeInTheDocument()
    expect(screen.getByText('Custo Médio (BRL)')).toBeInTheDocument()
    expect(screen.getByText('Custo Total (BRL)')).toBeInTheDocument()
    expect(screen.getByText('PTAX')).toBeInTheDocument()
    expect(screen.getByText('Ganho de Capital Ano (BRL)')).toBeInTheDocument()
    expect(screen.getByText('Ganho de Capital Total (BRL)')).toBeInTheDocument()
    expect(screen.getByText('Dividendos Bruto (BRL)')).toBeInTheDocument()
    expect(screen.getByText('Imposto Dividendos (BRL)')).toBeInTheDocument()
  })

  it('renders asset symbols', () => {
    render(<IrpfTable data={sampleData} />)
    expect(screen.getByText('AAPL')).toBeInTheDocument()
    expect(screen.getByText('MSFT')).toBeInTheDocument()
  })

  it('renders quantity as plain number', () => {
    render(<IrpfTable data={sampleData} />)
    expect(screen.getByText('12')).toBeInTheDocument()
    expect(screen.getByText('8')).toBeInTheDocument()
  })

  it('formats money fields as BRL currency', () => {
    render(<IrpfTable data={sampleData} />)
    expect(screen.getByText((_content, element) =>
      element?.tagName === 'TD' && /R\$\s*290,29/.test(element.textContent ?? '')
    )).toBeInTheDocument()
    expect(screen.getByText((_content, element) =>
      element?.tagName === 'TD' && /R\$\s*3\.483,48/.test(element.textContent ?? '')
    )).toBeInTheDocument()
  })

  it('formats USD cost fields', () => {
    render(<IrpfTable data={sampleData} />)
    expect(screen.getByText((_content, element) =>
      element?.tagName === 'TD' && /US\$\s*53,39/.test(element.textContent ?? '')
    )).toBeInTheDocument()
  })

  it('formats PTAX rate with 4 decimal places', () => {
    render(<IrpfTable data={sampleData} />)
    expect(screen.getByText('5,4369')).toBeInTheDocument()
  })

  it('renders empty state when data is empty', () => {
    render(<IrpfTable data={[]} />)
    expect(screen.getByText(/nenhum dado encontrado/i)).toBeInTheDocument()
  })

  it('renders correct number of rows', () => {
    render(<IrpfTable data={sampleData} />)
    const rows = screen.getAllByRole('row')
    expect(rows).toHaveLength(3) // header + 2 data rows
  })

  it('formats zero money values correctly', () => {
    render(<IrpfTable data={sampleData} />)
    const zeroCells = screen.getAllByText((_content, element) =>
      element?.tagName === 'TD' && /R\$\s*0,00/.test(element.textContent ?? '')
    )
    expect(zeroCells.length).toBeGreaterThan(0)
  })

  it('renders error row with clickable details for assets with errors', () => {
    const dataWithError: IrpfResponse = [
      { symbol: 'AAPL', quantity: 12, avgCostUsd: 53.39, totalCostUsd: 640.68, avgCostBrl: 290.29, totalCostBrl: 3483.48, ptaxRate: 5.4369, capitalGainsBrl: 328.23, totalCapitalGainsBrl: 328.23, dividendsGrossBrl: 4.53, dividendsTaxBrl: 0.30 },
      { symbol: 'MSFT', error: 'Sell amount exceeds current position on 2025-06-15' },
    ]
    render(<IrpfTable data={dataWithError} />)

    expect(screen.getByText('AAPL')).toBeInTheDocument()
    expect(screen.getByText('MSFT')).toBeInTheDocument()

    const errorSummary = screen.getByText(/erro/i)
    expect(errorSummary).toBeInTheDocument()

    expect(screen.getByText(/Sell amount exceeds current position/)).toBeInTheDocument()
  })

  it('renders mix of success and error rows with correct row count', () => {
    const dataWithError: IrpfResponse = [
      { symbol: 'AAPL', quantity: 12, avgCostUsd: 53.39, totalCostUsd: 640.68, avgCostBrl: 290.29, totalCostBrl: 3483.48, ptaxRate: 5.4369, capitalGainsBrl: 328.23, totalCapitalGainsBrl: 328.23, dividendsGrossBrl: 4.53, dividendsTaxBrl: 0.30 },
      { symbol: 'MSFT', error: 'Some error' },
    ]
    render(<IrpfTable data={dataWithError} />)
    const rows = screen.getAllByRole('row')
    expect(rows).toHaveLength(3) // header + 2 data rows
  })
})
