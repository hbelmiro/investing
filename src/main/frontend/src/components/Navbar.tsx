import { Link } from 'react-router-dom'

export function Navbar() {
  return (
    <nav className="navbar">
      <Link to="/" className="navbar-title">Investing</Link>
      <Link to="/irpf" className="navbar-link">IRPF</Link>
    </nav>
  )
}
