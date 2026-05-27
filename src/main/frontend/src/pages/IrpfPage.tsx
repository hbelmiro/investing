import { useEffect, useState } from 'react'
import { fetchBrIrpfData, fetchIrpfData, fetchIrpfYears } from '../api/client'
import type { IrpfResponse } from '../api/types'
import { IrpfTable } from '../components/IrpfTable'
import { BrIrpfTable } from '../components/BrIrpfTable'

const currentYear = new Date().getFullYear()

export function IrpfPage() {
  const [years, setYears] = useState<number[]>([currentYear])
  const [year, setYear] = useState(currentYear)
  const [usData, setUsData] = useState<IrpfResponse | null>(null)
  const [brData, setBrData] = useState<IrpfResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const abortController = new AbortController()
    fetchIrpfYears(abortController.signal)
      .then(y => { if (!abortController.signal.aborted) setYears(y) })
      .catch(() => {})
    return () => { abortController.abort() }
  }, [])

  useEffect(() => {
    const abortController = new AbortController()

    const loadData = async () => {
      setLoading(true)
      setError(null)
      try {
        const [usResult, brResult] = await Promise.all([
          fetchIrpfData(year, abortController.signal),
          fetchBrIrpfData(year, abortController.signal),
        ])
        if (!abortController.signal.aborted) {
          setUsData(usResult)
          setBrData(brResult)
        }
      } catch (e) {
        if (!abortController.signal.aborted) {
          if (e instanceof Error && e.name === 'AbortError') return
          setError(e instanceof Error ? e.message : 'Erro ao carregar dados')
        }
      } finally {
        if (!abortController.signal.aborted) {
          setLoading(false)
        }
      }
    }

    loadData()
    return () => { abortController.abort() }
  }, [year])

  return (
    <div>
      <div className="irpf-controls">
        <label htmlFor="year-select">Ano:</label>
        <select
          id="year-select"
          value={year}
          onChange={(e) => setYear(Number(e.target.value))}
        >
          {years.map((y) => (
            <option key={y} value={y}>{y}</option>
          ))}
        </select>
      </div>
      {loading && <p>Carregando...</p>}
      {error && <p className="error">Erro: {error}</p>}
      {!loading && !error && usData && brData && (
        <>
          <h2>Ações US</h2>
          <IrpfTable data={usData} />
          <h2>Ações BR e FIIs</h2>
          <BrIrpfTable data={brData} />
        </>
      )}
    </div>
  )
}
