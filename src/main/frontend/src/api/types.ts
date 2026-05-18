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
