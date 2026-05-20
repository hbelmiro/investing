import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { Layout } from './components/Layout'
import { Dashboard } from './pages/Dashboard'
import { IrpfPage } from './pages/IrpfPage'

export function App() {
  return (
    <BrowserRouter>
      <Layout>
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/irpf" element={<IrpfPage />} />
        </Routes>
      </Layout>
    </BrowserRouter>
  )
}
