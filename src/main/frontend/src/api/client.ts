import type { StocksResponse, IrpfResponse } from './types'

export type Market = 'brazil' | 'us'

const ENDPOINTS: Record<Market, string> = {
  brazil: '/brazil_stocks',
  us: '/us_stocks',
}

export async function fetchStocks(market: Market, signal?: AbortSignal): Promise<StocksResponse> {
  const response = await fetch(ENDPOINTS[market], { signal })
  if (!response.ok) {
    throw new Error(`Failed to fetch ${market} stocks: ${response.status} ${response.statusText}`)
  }
  return response.json()
}

export async function fetchIrpfYears(signal?: AbortSignal): Promise<number[]> {
  const response = await fetch('/api/irpf/years', { signal })
  if (!response.ok) {
    throw new Error(`Failed to fetch IRPF years: ${response.status} ${response.statusText}`)
  }
  return response.json()
}

export async function fetchIrpfData(year: number, signal?: AbortSignal): Promise<IrpfResponse> {
  const response = await fetch(`/api/irpf/us_stocks?year=${year}`, { signal })
  if (!response.ok) {
    throw new Error(`Failed to fetch IRPF data: ${response.status} ${response.statusText}`)
  }
  return response.json()
}

export async function fetchBrIrpfData(year: number, signal?: AbortSignal): Promise<IrpfResponse> {
  const response = await fetch(`/api/irpf/br_stocks?year=${year}`, { signal })
  if (!response.ok) {
    throw new Error(`Failed to fetch BR IRPF data: ${response.status} ${response.statusText}`)
  }
  return response.json()
}
