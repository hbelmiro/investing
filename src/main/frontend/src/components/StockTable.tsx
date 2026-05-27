import { useState } from 'react'
import { COLUMN_HEADERS, type StocksResponse } from '../api/types'

interface StockTableProps {
  readonly data: StocksResponse
}

export function StockTable({ data }: StockTableProps) {
  const [selectedKey, setSelectedKey] = useState<string | null>(null)

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
          <tr
            key={row[0]}
            className={selectedKey === row[0] ? 'selected' : undefined}
            onClick={() => setSelectedKey(selectedKey === row[0] ? null : row[0])}
          >
            {row.map((cell, i) => (
              <td key={`${row[0]}-${i}`}>{cell}</td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  )
}
