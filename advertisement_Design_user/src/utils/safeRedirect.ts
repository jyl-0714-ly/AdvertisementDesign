export function sanitizeInternalRedirect(value: unknown, fallback = '/workspace/new') {
  if (typeof value !== 'string' || !value.startsWith('/') || value.startsWith('//')) return fallback
  try {
    const url = new URL(value, window.location.origin)
    if (url.origin !== window.location.origin || url.pathname === '/login') return fallback
    return `${url.pathname}${url.search}${url.hash}`
  } catch {
    return fallback
  }
}
