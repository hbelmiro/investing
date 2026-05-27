import { useState } from 'react'
import { IRPF_COLUMN_HEADERS, type IrpfAssetData, type IrpfResponse } from '../api/types'
import { brlFormatter, quantityFormatter } from '../formatters'

interface IrpfTableProps {
  readonly data: IrpfResponse
}

const usdFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'USD',
})

const ptaxFormatter = new Intl.NumberFormat('pt-BR', {
  minimumFractionDigits: 4,
  maximumFractionDigits: 4,
})

export function IrpfTable({ data }: IrpfTableProps) {
  const [selectedKey, setSelectedKey] = useState<string | null>(null)

  if (data.length === 0) {
    return <p>Nenhum dado encontrado.</p>
  }

  return (
    <table>
      <thead>
        <tr>
          {IRPF_COLUMN_HEADERS.map((header) => (
            <th key={header}>{header}</th>
          ))}
        </tr>
      </thead>
      <tbody>
        {data.map((row) => (
          <IrpfRow
            key={row.symbol}
            row={row}
            selected={selectedKey === row.symbol}
            onSelect={() => setSelectedKey(selectedKey === row.symbol ? null : row.symbol)}
          />
        ))}
      </tbody>
    </table>
  )
}

interface IrpfRowProps {
  readonly row: IrpfAssetData
  readonly selected: boolean
  readonly onSelect: () => void
}

function IrpfRow({ row, selected, onSelect }: IrpfRowProps) {
  if (row.error) {
    return (
      <tr className={`irpf-error-row${selected ? ' selected' : ''}`} onClick={onSelect}>
        <td>{row.symbol}</td>
        <td colSpan={10}>
          <details className="irpf-error-details">
            <summary>Erro</summary>
            <p>{row.error}</p>
          </details>
        </td>
      </tr>
    )
  }

  return (
    <tr className={selected ? 'selected' : undefined} onClick={onSelect}>
      <td>{row.symbol}</td>
      <td>{quantityFormatter.format(row.quantity!)}</td>
      <td>{usdFormatter.format(row.avgCostUsd!)}</td>
      <td>{usdFormatter.format(row.totalCostUsd!)}</td>
      <td>{brlFormatter.format(row.avgCostBrl!)}</td>
      <td>{brlFormatter.format(row.totalCostBrl!)}</td>
      <td>{ptaxFormatter.format(row.ptaxRate!)}</td>
      <td>{brlFormatter.format(row.capitalGainsBrl!)}</td>
      <td>{brlFormatter.format(row.totalCapitalGainsBrl!)}</td>
      <td>{brlFormatter.format(row.dividendsGrossBrl!)}</td>
      <td>{brlFormatter.format(row.dividendsTaxBrl!)}</td>
    </tr>
  )
}
