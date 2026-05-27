import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { BrIrpfTable } from './BrIrpfTable'
import type { IrpfResponse } from '../api/types'

describe('BrIrpfTable', () => {
  const sampleData: IrpfResponse = [
    { symbol: 'PETR4', quantity: 120, avgCostBrl: 31.67, totalCostBrl: 3800.40, capitalGainsBrl: 249.84, totalCapitalGainsBrl: 249.84, dividendsGrossBrl: 1.50, dividendsTaxBrl: 0 },
    { symbol: 'MXRF11', quantity: 200, avgCostBrl: 10.50, totalCostBrl: 2100.00, capitalGainsBrl: 0, totalCapitalGainsBrl: 0, dividendsGrossBrl: 45.00, dividendsTaxBrl: 0 },
  ]

  it('renders all 8 column headers', () => {
    render(<BrIrpfTable data={sampleData} />)
    expect(screen.getByText('Ativo')).toBeInTheDocument()
    expect(screen.getByText('Quantidade')).toBeInTheDocument()
    expect(screen.getByText('Custo Médio (BRL)')).toBeInTheDocument()
    expect(screen.getByText('Custo Total (BRL)')).toBeInTheDocument()
    expect(screen.getByText('Ganho de Capital Ano (BRL)')).toBeInTheDocument()
    expect(screen.getByText('Ganho de Capital Total (BRL)')).toBeInTheDocument()
    expect(screen.getByText('Dividendos Bruto (BRL)')).toBeInTheDocument()
    expect(screen.getByText('Imposto Dividendos (BRL)')).toBeInTheDocument()
  })

  it('does not render USD or PTAX headers', () => {
    render(<BrIrpfTable data={sampleData} />)
    expect(screen.queryByText('Custo Médio (USD)')).not.toBeInTheDocument()
    expect(screen.queryByText('Custo Total (USD)')).not.toBeInTheDocument()
    expect(screen.queryByText('PTAX')).not.toBeInTheDocument()
  })

  it('renders asset symbols', () => {
    render(<BrIrpfTable data={sampleData} />)
    expect(screen.getByText('PETR4')).toBeInTheDocument()
    expect(screen.getByText('MXRF11')).toBeInTheDocument()
  })

  it('renders quantity with Brazilian formatting', () => {
    render(<BrIrpfTable data={sampleData} />)
    expect(screen.getByText('120')).toBeInTheDocument()
    expect(screen.getByText('200')).toBeInTheDocument()
  })

  it('formats fractional quantity with Brazilian locale and near-zero as zero', () => {
    const data: IrpfResponse = [
      { symbol: 'PETR4', quantity: 1.2437552, avgCostBrl: 31.67, totalCostBrl: 3800.4, capitalGainsBrl: 0, totalCapitalGainsBrl: 0, dividendsGrossBrl: 0, dividendsTaxBrl: 0 },
      { symbol: 'VALE3', quantity: 2.2e-16, avgCostBrl: 0, totalCostBrl: 0, capitalGainsBrl: 0, totalCapitalGainsBrl: 0, dividendsGrossBrl: 0, dividendsTaxBrl: 0 },
    ]
    render(<BrIrpfTable data={data} />)
    expect(screen.getByText('1,2437552')).toBeInTheDocument()
    expect(screen.queryByText(/e-/)).not.toBeInTheDocument()
  })

  it('formats money fields as BRL currency', () => {
    render(<BrIrpfTable data={sampleData} />)
    expect(screen.getByText((_content, element) =>
      element?.tagName === 'TD' && /R\$\s*31,67/.test(element.textContent ?? '')
    )).toBeInTheDocument()
    expect(screen.getByText((_content, element) =>
      element?.tagName === 'TD' && /R\$\s*3\.800,40/.test(element.textContent ?? '')
    )).toBeInTheDocument()
  })

  it('renders empty state when data is empty', () => {
    render(<BrIrpfTable data={[]} />)
    expect(screen.getByText(/nenhum dado encontrado/i)).toBeInTheDocument()
  })

  it('renders correct number of rows', () => {
    render(<BrIrpfTable data={sampleData} />)
    const rows = screen.getAllByRole('row')
    expect(rows).toHaveLength(3) // header + 2 data rows
  })

  it('renders error row with clickable details', () => {
    const dataWithError: IrpfResponse = [
      { symbol: 'PETR4', quantity: 120, avgCostBrl: 31.67, totalCostBrl: 3800.40, capitalGainsBrl: 249.84, totalCapitalGainsBrl: 249.84, dividendsGrossBrl: 1.50, dividendsTaxBrl: 0 },
      { symbol: 'VALE3', error: 'Sell amount exceeds position' },
    ]
    render(<BrIrpfTable data={dataWithError} />)

    expect(screen.getByText('PETR4')).toBeInTheDocument()
    expect(screen.getByText('VALE3')).toBeInTheDocument()
    expect(screen.getByText(/erro/i)).toBeInTheDocument()
    expect(screen.getByText(/Sell amount exceeds position/)).toBeInTheDocument()
  })
})
