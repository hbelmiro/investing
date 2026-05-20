import { IRPF_COLUMN_HEADERS, type IrpfAssetData, type IrpfResponse } from '../api/types'

interface IrpfTableProps {
  data: IrpfResponse
}

const brlFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
})

const usdFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'USD',
})

const ptaxFormatter = new Intl.NumberFormat('pt-BR', {
  minimumFractionDigits: 4,
  maximumFractionDigits: 4,
})

export function IrpfTable({ data }: IrpfTableProps) {
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
          <IrpfRow key={row.symbol} row={row} />
        ))}
      </tbody>
    </table>
  )
}

function IrpfRow({ row }: { row: IrpfAssetData }) {
  if (row.error) {
    return (
      <tr className="irpf-error-row">
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
    <tr>
      <td>{row.symbol}</td>
      <td>{row.quantity}</td>
      <td>{usdFormatter.format(row.avgCostUsd!)}</td>
      <td>{usdFormatter.format(row.totalCostUsd!)}</td>
      <td>{brlFormatter.format(row.avgCostBrl!)}</td>
      <td>{brlFormatter.format(row.totalCostBrl!)}</td>
      <td>{ptaxFormatter.format(row.ptaxRate!)}</td>
      <td>{brlFormatter.format(row.capitalGainsBrl!)}</td>
      <td>{brlFormatter.format(row.totalCapitalGainsBrl!)}</td>
      <td>{brlFormatter.format(row.totalDividendsBrl!)}</td>
    </tr>
  )
}
