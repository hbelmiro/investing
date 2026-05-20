export type StockRow = [string, string, string, string, string, string]

export type StocksResponse = StockRow[]

export const COLUMN_HEADERS = [
  'Ativo',
  'Quantidade',
  'Preço Médio',
  'Preço Atual',
  'Lucro',
  'Proventos',
] as const

export interface IrpfAssetData {
  symbol: string
  quantity?: number
  avgCostBrl?: number
  totalCostBrl?: number
  avgCostUsd?: number
  totalCostUsd?: number
  ptaxRate?: number
  capitalGainsBrl?: number
  totalCapitalGainsBrl?: number
  totalDividendsBrl?: number
  error?: string
}

export type IrpfResponse = IrpfAssetData[]

export const IRPF_COLUMN_HEADERS = [
  'Ativo',
  'Quantidade',
  'Custo Médio (USD)',
  'Custo Total (USD)',
  'Custo Médio (BRL)',
  'Custo Total (BRL)',
  'PTAX',
  'Ganho de Capital Ano (BRL)',
  'Ganho de Capital Total (BRL)',
  'Dividendos (BRL)',
] as const
