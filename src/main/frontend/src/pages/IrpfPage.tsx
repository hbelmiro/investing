import { useEffect, useState } from 'react'
import { fetchIrpfData, fetchIrpfYears } from '../api/client'
import type { IrpfResponse } from '../api/types'
import { IrpfTable } from '../components/IrpfTable'

const currentYear = new Date().getFullYear()

export function IrpfPage() {
  const [years, setYears] = useState<number[]>([currentYear])
  const [year, setYear] = useState(currentYear)
  const [data, setData] = useState<IrpfResponse | null>(null)
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
        const result = await fetchIrpfData(year, abortController.signal)
        if (!abortController.signal.aborted) {
          setData(result)
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
      {!loading && !error && data && <IrpfTable data={data} />}
    </div>
  )
}
