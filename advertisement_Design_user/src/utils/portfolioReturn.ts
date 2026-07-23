const STORAGE_KEY = 'ad-user-portfolio-return'
const MAX_AGE_MS = 30 * 60 * 1000

interface PortfolioReturnLocation {
  path: string
  scrollY: number
  createdAt: number
}

export function rememberPortfolioPosition(path: string) {
  const location: PortfolioReturnLocation = {
    path,
    scrollY: window.scrollY,
    createdAt: Date.now()
  }
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(location))
}

export function getPortfolioReturnLocation() {
  const value = sessionStorage.getItem(STORAGE_KEY)
  if (!value) return null

  try {
    const location = JSON.parse(value) as PortfolioReturnLocation
    if (!location.path || Date.now() - location.createdAt > MAX_AGE_MS) {
      sessionStorage.removeItem(STORAGE_KEY)
      return null
    }
    return location
  } catch {
    sessionStorage.removeItem(STORAGE_KEY)
    return null
  }
}

export function restorePortfolioPosition(path: string) {
  const location = getPortfolioReturnLocation()
  if (!location || location.path !== path) return

  sessionStorage.removeItem(STORAGE_KEY)
  window.requestAnimationFrame(() => {
    window.requestAnimationFrame(() => window.scrollTo({ top: location.scrollY, behavior: 'auto' }))
  })
}
