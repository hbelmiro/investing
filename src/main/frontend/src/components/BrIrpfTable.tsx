import { BR_IRPF_COLUMN_HEADERS, type IrpfAssetData, type IrpfResponse } from '../api/types'
import { brlFormatter, quantityFormatter } from '../formatters'

interface BrIrpfTableProps {
  data: IrpfResponse
}

export function BrIrpfTable({ data }: BrIrpfTableProps) {
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
          <BrIrpfRow key={row.symbol} row={row} />
        ))}
      </tbody>
    </table>
  )
}

function BrIrpfRow({ row }: { row: IrpfAssetData }) {
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
