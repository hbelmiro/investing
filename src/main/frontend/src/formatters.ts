export const brlFormatter = new Intl.NumberFormat('pt-BR', {
  style: 'currency',
  currency: 'BRL',
})

export const quantityFormatter = new Intl.NumberFormat('pt-BR', {
  maximumFractionDigits: 10,
})
