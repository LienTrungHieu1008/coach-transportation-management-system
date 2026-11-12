export async function checkSystemHealth(signal: AbortSignal): Promise<void> {
  const response = await fetch('/api/health', {
    credentials: 'same-origin',
    headers: { Accept: 'application/json' },
    signal,
  })
  if (!response.ok) throw new Error('Service unavailable')

  const data: unknown = await response.json()
  if (typeof data !== 'object' || data === null || !('status' in data) || data.status !== 'UP') {
    throw new Error('Invalid health response')
  }
}
