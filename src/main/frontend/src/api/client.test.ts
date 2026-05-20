import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fetchStocks, fetchIrpfData, fetchIrpfYears } from './client'
import type { StocksResponse, IrpfResponse } from './types'

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
    expect(fetch).toHaveBeenCalledWith('/brazil_stocks', { signal: undefined })
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
    expect(fetch).toHaveBeenCalledWith('/us_stocks', { signal: undefined })
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

describe('fetchIrpfData', () => {
  const mockData: IrpfResponse = [
    { symbol: 'AAPL', quantity: 12, avgCostBrl: 290.29, totalCostBrl: 3483.48, capitalGainsBrl: 328.23, totalDividendsBrl: 3.71 },
  ]

  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('fetches IRPF data for a given year and returns parsed data', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockData),
    } as Response)

    const result = await fetchIrpfData(2025)
    expect(result).toEqual(mockData)
    expect(fetch).toHaveBeenCalledWith('/api/irpf/us_stocks?year=2025', { signal: undefined })
  })

  it('passes AbortSignal to fetch', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockData),
    } as Response)

    const controller = new AbortController()
    await fetchIrpfData(2025, controller.signal)
    expect(fetch).toHaveBeenCalledWith('/api/irpf/us_stocks?year=2025', { signal: controller.signal })
  })

  it('throws on non-ok response', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: false,
      status: 500,
      statusText: 'Internal Server Error',
    } as Response)

    await expect(fetchIrpfData(2025)).rejects.toThrow('Failed to fetch IRPF data: 500 Internal Server Error')
  })
})

describe('fetchIrpfYears', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('fetches available years and returns parsed data', async () => {
    const mockYears = [2020, 2021, 2022, 2023, 2024, 2025]
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockYears),
    } as Response)

    const result = await fetchIrpfYears()
    expect(result).toEqual(mockYears)
    expect(fetch).toHaveBeenCalledWith('/api/irpf/years', { signal: undefined })
  })

  it('throws on non-ok response', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: false,
      status: 500,
      statusText: 'Internal Server Error',
    } as Response)

    await expect(fetchIrpfYears()).rejects.toThrow('Failed to fetch IRPF years: 500 Internal Server Error')
  })
})
