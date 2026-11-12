import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import App from './App'

function healthResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('foundation connection status', () => {
  it('shows loading while health is not yet known', () => {
    vi.stubGlobal('fetch', () => new Promise<Response>(() => {}))
    render(<App />)
    expect(screen.getByRole('status').textContent).toContain('Đang kiểm tra kết nối')
  })

  it('shows ready only after an UP response', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(healthResponse({ status: 'UP' })))
    render(<App />)
    expect((await screen.findByText('Kết nối sẵn sàng')).textContent).toBe('Kết nối sẵn sàng')
    expect(screen.getByText(/chưa phải bản demo nghiệp vụ PA4/).textContent).toContain('chưa phải bản demo nghiệp vụ PA4')
  })

  it('allows retry after a network failure', async () => {
    vi.stubGlobal('fetch', vi.fn()
      .mockRejectedValueOnce(new TypeError('network unavailable'))
      .mockResolvedValueOnce(healthResponse({ status: 'UP' })))
    render(<App />)
    expect((await screen.findByText('Chưa kết nối được dịch vụ')).textContent).toBe('Chưa kết nối được dịch vụ')
    fireEvent.click(screen.getByRole('button', { name: 'Thử lại' }))
    expect((await screen.findByText('Kết nối sẵn sàng')).textContent).toBe('Kết nối sẵn sàng')
  })

  it('does not claim success for an HTTP error', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(healthResponse({ status: 'DOWN' }, 503)))
    render(<App />)
    expect((await screen.findByText('Chưa kết nối được dịch vụ')).textContent).toBe('Chưa kết nối được dịch vụ')
    expect(screen.queryByText('Kết nối sẵn sàng')).toBeNull()
  })

  it('does not claim success for malformed successful data', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(healthResponse({ message: 'UP' })))
    render(<App />)
    expect((await screen.findByText('Chưa kết nối được dịch vụ')).textContent).toBe('Chưa kết nối được dịch vụ')
  })
})
