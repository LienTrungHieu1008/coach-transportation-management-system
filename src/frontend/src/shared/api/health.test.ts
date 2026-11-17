import { describe, expect, it, vi } from 'vitest'
import { checkSystemHealth } from './health'

describe('foundation health API client', () => {
  it('accepts UP', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('{"status":"UP"}')))
    await expect(checkSystemHealth(new AbortController().signal)).resolves.toBeUndefined()
  })

  it('propagates network errors', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('network unavailable')))
    await expect(checkSystemHealth(new AbortController().signal)).rejects.toThrow('network unavailable')
  })

  it('rejects HTTP errors even with an UP body', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('{"status":"UP"}', { status: 503 })))
    await expect(checkSystemHealth(new AbortController().signal)).rejects.toThrow('Service unavailable')
  })

  it.each(['{"message":"UP"}', '{"status":"DOWN"}', 'null', 'invalid JSON'])(
    'rejects invalid payload %s', async (body) => {
      vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(body)))
      await expect(checkSystemHealth(new AbortController().signal)).rejects.toThrow()
    },
  )
})
