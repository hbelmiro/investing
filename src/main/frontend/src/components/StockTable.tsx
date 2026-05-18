import { COLUMN_HEADERS, type StocksResponse } from '../api/types'

interface StockTableProps {
  data: StocksResponse
}

export function StockTable({ data }: StockTableProps) {
  if (data.length === 0) {
    return <p>Nenhum ativo encontrado.</p>
  }

  return (
    <table>
      <thead>
        <tr>
          {COLUMN_HEADERS.map((header) => (
            <th key={header}>{header}</th>
          ))}
        </tr>
      </thead>
      <tbody>
        {data.map((row) => (
          <tr key={row[0]}>
            {row.map((cell, i) => (
              <td key={i}>{cell}</td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  )
}
