import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fetchStocks } from './client'
import type { StocksResponse } from './types'

describe('fetchStocks', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('fetches Brazil stocks and returns parsed data', async () => {
    const mockData: StocksResponse = [
      ['PETR4', '100', 'R$ 30,00', 'R$ 35,00', '16,67%', 'R$ 200,00'],
    ]
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockData),
    } as Response)

    const result = await fetchStocks('brazil')
    expect(result).toEqual(mockData)
    expect(fetch).toHaveBeenCalledWith('/brazil_stocks')
  })

  it('fetches US stocks and returns parsed data', async () => {
    const mockData: StocksResponse = [
      ['AAPL', '10', 'US$ 150,00', 'US$ 170,00', '13,33%', 'US$ 5,00'],
    ]
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockData),
    } as Response)

    const result = await fetchStocks('us')
    expect(result).toEqual(mockData)
    expect(fetch).toHaveBeenCalledWith('/us_stocks')
  })

  it('throws on non-ok response', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: false,
      status: 500,
      statusText: 'Internal Server Error',
    } as Response)

    await expect(fetchStocks('brazil')).rejects.toThrow()
  })
})
