import { useEffect, useState, useCallback, KeyboardEvent } from 'react'
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
    setActiveTab(market)
  }

  const handleKeyDown = (event: KeyboardEvent<HTMLButtonElement>, index: number) => {
    if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') {
      event.preventDefault()
      const nextIndex = event.key === 'ArrowLeft'
        ? (index - 1 + TABS.length) % TABS.length
        : (index + 1) % TABS.length

      const nextTab = TABS[nextIndex]
      handleTabChange(nextTab.key)

      // Focus the next tab
      const tabElement = document.querySelector(`[data-tab="${nextTab.key}"]`) as HTMLElement
      tabElement?.focus()
    }
  }

  return (
    <div>
      <div role="tablist" className="tabs">
        {TABS.map((tab, index) => (
          <button
            key={tab.key}
            role="tab"
            data-tab={tab.key}
            aria-selected={activeTab === tab.key}
            aria-controls="stock-tabpanel"
            tabIndex={activeTab === tab.key ? 0 : -1}
            className={`tab ${activeTab === tab.key ? 'tab-active' : ''}`}
            onClick={() => handleTabChange(tab.key)}
            onKeyDown={(e) => handleKeyDown(e, index)}
          >
            {tab.label}
          </button>
        ))}
      </div>
      <div
        id="stock-tabpanel"
        role="tabpanel"
        className="tab-panel"
        aria-labelledby={`${activeTab}-tab`}
      >
        {loading && <p>Carregando...</p>}
        {error && <p className="error">Erro: {error}</p>}
        {!loading && !error && data && <StockTable data={data} />}
      </div>
    </div>
  )
}
