import { describe, it, expect } from 'vitest'
import { quantityFormatter, brlFormatter } from './formatters'

describe('quantityFormatter', () => {
  it('uses comma as decimal separator for fractional values', () => {
    expect(quantityFormatter.format(1.2437552)).toBe('1,2437552')
  })

  it('renders near-zero values as zero instead of scientific notation', () => {
    expect(quantityFormatter.format(2.2e-16)).toBe('0')
  })

  it('renders whole numbers without decimals', () => {
    expect(quantityFormatter.format(12)).toBe('12')
  })
})

describe('brlFormatter', () => {
  it('formats as BRL currency', () => {
    expect(brlFormatter.format(290.29)).toMatch(/R\$\s*290,29/)
  })
})
