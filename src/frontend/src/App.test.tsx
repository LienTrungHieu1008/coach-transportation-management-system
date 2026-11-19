import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'
import { availability, details, page, response, stops, trip } from './test/journeyFixtures'

const fetcher = vi.fn()
beforeEach(() => {
  fetcher.mockReset().mockImplementation(async (input: string) => {
    const url = new URL(input, 'http://localhost')
    if (url.pathname === '/api/health') return response({ status: 'UP' })
    if (url.pathname === '/api/v1/stops') return response({ items: stops, serverTime: details.serverTime })
    if (url.pathname === '/api/v1/trips') return response({ ...page, page: Number(url.searchParams.get('page')) })
    if (url.pathname.endsWith('/seat-availability')) return response(availability)
    return response(details)
  })
  vi.stubGlobal('fetch', fetcher)
})
async function search() {
  await within(screen.getByLabelText('Điểm đón')).findByRole('option', { name: 'Demo Stop C' })
  fireEvent.change(screen.getByLabelText('Điểm đón'), { target: { value: stops[0].id } })
  fireEvent.change(screen.getByLabelText('Điểm trả'), { target: { value: stops[1].id } })
  fireEvent.change(screen.getByLabelText('Ngày lên xe'), { target: { value: '2026-11-20' } })
  fireEvent.submit(screen.getByLabelText('Điểm đón').closest('form')!)
}
async function openDetails() {
  await search()
  fireEvent.click(await screen.findByRole('button', { name: /Xem chi tiết/ }))
  await screen.findByRole('heading', { name: 'Các điểm dừng theo thứ tự' })
}
describe('public journey connected to API', () => {
  it('loads three Stops and hides Schedule and prototype controls', async () => {
    render(<App />)
    await within(screen.getByLabelText('Điểm đón')).findByRole('option', { name: 'Demo Stop C' })
    expect(screen.queryByRole('button', { name: 'Lịch trình' })).toBeNull()
    expect(screen.queryByText(/Preview nhánh/)).toBeNull()
  })
  it('preserves Trip and Segment through details, seats and back', async () => {
    render(<App />); await openDetails()
    fireEvent.click(screen.getByRole('button', { name: /Xem tình trạng ghế/ }))
    await screen.findByLabelText('Ghế A1: Còn trống')
    expect(screen.getByLabelText('Ghế A2: Không khả dụng')).toBeTruthy()
    expect(fetcher.mock.calls.map(c => c[0]).some(url => String(url).includes(`${trip.tripId}/seat-availability?fromStopId=${stops[0].id}&toStopId=${stops[1].id}`))).toBe(true)
    fireEvent.click(screen.getByRole('button', { name: /Quay lại/ }))
    await screen.findByRole('button', { name: /Xem tình trạng ghế/ })
  })
  it('rejects identical Stops before calling search API', async () => {
    render(<App />); await within(screen.getByLabelText('Điểm đón')).findByRole('option', { name: 'Demo Stop C' })
    fireEvent.change(screen.getByLabelText('Điểm đón'), { target: { value: stops[0].id } })
    fireEvent.change(screen.getByLabelText('Điểm trả'), { target: { value: stops[0].id } })
    fireEvent.submit(screen.getByLabelText('Điểm đón').closest('form')!)
    expect(screen.getByRole('alert').textContent).toContain('khác nhau')
    expect(fetcher.mock.calls.some(c => String(c[0]).includes('/trips?'))).toBe(false)
  })
  it('shows empty results from the server', async () => {
    fetcher.mockImplementation(async (input: string) => input.includes('/trips?')
      ? response({ ...page, items: [], totalElements: 0, totalPages: 0 }) : response({ items: stops, serverTime: details.serverTime }))
    render(<App />); await search()
    await screen.findByRole('heading', { name: 'Không có chuyến phù hợp' })
  })
  it('renders actual HTTP search failure and retries', async () => {
    const base = fetcher.getMockImplementation()!
    let failed = false
    fetcher.mockImplementation(async (input: string) => {
      if (input.includes('/trips?') && !failed) { failed = true; return response({ code: 'TRIP_DATA_UNAVAILABLE', message: 'Không thể xác minh chuyến xe', fieldErrors: [] }, 503) }
      return base(input)
    })
    render(<App />); await search()
    await screen.findByRole('alert')
    fireEvent.click(screen.getByRole('button', { name: 'Thử lại' }))
    await screen.findByRole('button', { name: /Xem chi tiết/ })
  })
  it('preserves metadata and renders a second search page', async () => {
    const base = fetcher.getMockImplementation()!
    fetcher.mockImplementation(async (input: string) => input.includes('/trips?')
      ? response({ ...page, page: Number(new URL(input, 'http://localhost').searchParams.get('page')), totalElements: 21, totalPages: 2 }) : base(input))
    render(<App />); await search()
    fireEvent.click(await screen.findByRole('button', { name: 'Trang sau' }))
    await waitFor(() => expect(screen.getByText(/Trang 2\/2/)).toBeTruthy())
    expect(fetcher.mock.calls.some(c => String(c[0]).includes('page=1'))).toBe(true)
  })
  it.each(['TRIP_NOT_SCHEDULED', 'BOOKING_CLOSED', 'ESSENTIAL_DATA_INCOMPLETE'])('blocks continuation for %s and preserves facts', async reason => {
    const base = fetcher.getMockImplementation()!
    fetcher.mockImplementation(async (input: string) => input.includes(`/trips/${trip.tripId}`)
      ? response({ ...details, bookingEligibility: { allowed: false, reasons: [reason] } }) : base(input))
    render(<App />); await openDetails()
    expect(screen.getByRole<HTMLButtonElement>('button', { name: /Xem tình trạng ghế/ }).disabled).toBe(true)
    expect(screen.getAllByText(/100\.000/).length).toBeGreaterThan(0)
  })
  it('renders nullable partial details without crashing', async () => {
    const base = fetcher.getMockImplementation()!
    fetcher.mockImplementation(async (input: string) => input.includes(`/trips/${trip.tripId}`)
      ? response({ ...details, route: null, vehicle: null, selectedSegment: null, stops: [], legFares: [], bookingEligibility: { allowed: false, reasons: ['ESSENTIAL_DATA_INCOMPLETE'] } }) : base(input))
    render(<App />); await openDetails()
    expect(screen.getByText('Chưa có giá')).toBeTruthy()
  })
  it('shows loading and ignores late responses after navigating away', async () => {
    const base = fetcher.getMockImplementation()!
    let resolve: ((value: ReturnType<typeof response>) => void) | undefined
    fetcher.mockImplementation((input: string) => input.includes('/trips?') ? new Promise(r => { resolve = r }) : base(input))
    render(<App />); await search()
    await screen.findByText('Đang tìm chuyến')
    fireEvent.click(screen.getByRole('button', { name: /Quay lại/ }))
    await act(async () => resolve?.(response(page)))
    expect(screen.queryByRole('button', { name: /Xem chi tiết/ })).toBeNull()
    expect(within(screen.getByLabelText('Điểm đón').closest('form')!).getByRole('button', { name: /Tìm chuyến/ })).toBeTruthy()
  })
})
