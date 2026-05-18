import type { StocksResponse } from './types'

export type Market = 'brazil' | 'us'

const ENDPOINTS: Record<Market, string> = {
  brazil: '/brazil_stocks',
  us: '/us_stocks',
}

export async function fetchStocks(market: Market): Promise<StocksResponse> {
  const response = await fetch(ENDPOINTS[market])
  if (!response.ok) {
    throw new Error(`Failed to fetch ${market} stocks: ${response.status} ${response.statusText}`)
  }
  return response.json()
}
