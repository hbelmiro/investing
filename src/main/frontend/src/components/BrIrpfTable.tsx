import { useState } from 'react'
import { BR_IRPF_COLUMN_HEADERS, type IrpfAssetData, type IrpfResponse } from '../api/types'
import { brlFormatter, quantityFormatter } from '../formatters'

interface BrIrpfTableProps {
  data: IrpfResponse
}

export function BrIrpfTable({ data }: BrIrpfTableProps) {
  const [selectedKey, setSelectedKey] = useState<string | null>(null)

  if (data.length === 0) {
    return <p>Nenhum dado encontrado.</p>
  }

  return (
    <table>
      <thead>
        <tr>
          {BR_IRPF_COLUMN_HEADERS.map((header) => (
            <th key={header}>{header}</th>
          ))}
        </tr>
      </thead>
      <tbody>
        {data.map((row) => (
          <BrIrpfRow
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

interface BrIrpfRowProps {
  readonly row: IrpfAssetData
  readonly selected: boolean
  readonly onSelect: () => void
}

function BrIrpfRow({ row, selected, onSelect }: BrIrpfRowProps) {
  if (row.error) {
    return (
      <tr className={`irpf-error-row${selected ? ' selected' : ''}`} onClick={onSelect}>
        <td>{row.symbol}</td>
        <td colSpan={9}>
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
      <td>{brlFormatter.format(row.avgCostBrl!)}</td>
      <td>{brlFormatter.format(row.totalCostBrl!)}</td>
      <td>{brlFormatter.format(row.capitalGainsBrl!)}</td>
      <td>{brlFormatter.format(row.totalCapitalGainsBrl!)}</td>
      <td>{brlFormatter.format(row.dividendsGrossBrl!)}</td>
      <td>{brlFormatter.format(row.dividendsTaxBrl!)}</td>
      <td>{brlFormatter.format(row.jcpGrossBrl!)}</td>
      <td>{brlFormatter.format(row.unknownGrossBrl!)}</td>
    </tr>
  )
}
