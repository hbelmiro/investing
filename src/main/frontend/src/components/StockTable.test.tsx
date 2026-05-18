import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { StockTable } from './StockTable'
import type { StocksResponse } from '../api/types'

describe('StockTable', () => {
  const sampleData: StocksResponse = [
    ['PETR4', '1.234', 'R$ 100,00', 'R$ 120,00', '20,00%', 'R$ 50,00'],
    ['VALE3', '500', 'R$ 80,00', 'R$ 90,00', '12,50%', 'R$ 30,00'],
  ]

  it('renders all column headers', () => {
    render(<StockTable data={sampleData} />)
    expect(screen.getByText('Ativo')).toBeInTheDocument()
    expect(screen.getByText('Quantidade')).toBeInTheDocument()
    expect(screen.getByText('Preço Médio')).toBeInTheDocument()
    expect(screen.getByText('Preço Atual')).toBeInTheDocument()
    expect(screen.getByText('Lucro')).toBeInTheDocument()
    expect(screen.getByText('Proventos')).toBeInTheDocument()
  })

  it('renders all data rows', () => {
    render(<StockTable data={sampleData} />)
    expect(screen.getByText('PETR4')).toBeInTheDocument()
    expect(screen.getByText('VALE3')).toBeInTheDocument()
    expect(screen.getByText('R$ 120,00')).toBeInTheDocument()
    expect(screen.getByText('20,00%')).toBeInTheDocument()
  })

  it('renders empty state when data is empty', () => {
    render(<StockTable data={[]} />)
    expect(screen.getByText(/nenhum ativo/i)).toBeInTheDocument()
  })

  it('renders correct number of rows', () => {
    render(<StockTable data={sampleData} />)
    const rows = screen.getAllByRole('row')
    expect(rows).toHaveLength(3) // header + 2 data rows
  })
})
