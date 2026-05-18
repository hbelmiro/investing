import { useEffect, useState, useCallback } from 'react'
import { fetchStocks, type Market } from '../api/client'
import type { StocksResponse } from '../api/types'
import { StockTable } from '../components/StockTable'

const TABS: { key: Market; label: string }[] = [
  { key: 'brazil', label: 'Brazil Stocks' },
  { key: 'us', label: 'US Stocks' },
]

export function Dashboard() {
  const [activeTab, setActiveTab] = useState<Market>('brazil')
  const [data, setData] = useState<StocksResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadData = useCallback(async (market: Market) => {
    setLoading(true)
    setError(null)
    try {
      const result = await fetchStocks(market)
      setData(result)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erro ao carregar dados')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadData(activeTab)
  }, [activeTab, loadData])

  const handleTabChange = (market: Market) => {
    setData(null)
    setActiveTab(market)
  }

  return (
    <div>
      <div role="tablist" className="tabs">
        {TABS.map((tab) => (
          <button
            key={tab.key}
            role="tab"
            aria-selected={activeTab === tab.key}
            className={`tab ${activeTab === tab.key ? 'tab-active' : ''}`}
            onClick={() => handleTabChange(tab.key)}
          >
            {tab.label}
          </button>
        ))}
      </div>
      <div role="tabpanel" className="tab-panel">
        {loading && <p>Carregando...</p>}
        {error && <p className="error">Erro: {error}</p>}
        {!loading && !error && data && <StockTable data={data} />}
      </div>
    </div>
  )
}
