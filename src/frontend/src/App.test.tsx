import { fireEvent, render, screen, within } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from './App'

function search() {
  const form = screen.getByLabelText('Điểm đón').closest('form')
  if (!form) throw new Error('Search form is missing')
  fireEvent.click(within(form).getByRole('button', { name: 'Tìm chuyến' }))
}

function openDetails() {
  search()
  fireEvent.click(screen.getByRole('button', { name: /Xem chi tiết/ }))
}

describe('public journey prototype', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date('2026-11-21T03:00:00Z'))
  })
  afterEach(() => vi.useRealTimers())

  it('keeps sample search usable after its real date, with a simulated clock label', () => {
    render(<App />)
    expect(screen.getByLabelText<HTMLInputElement>('Ngày lên xe').validity.rangeUnderflow).toBe(false)
    expect(screen.getByText(/Ngày mô phỏng: 20\/11\/2026/)).toBeTruthy()
    search()
    expect(screen.getByRole('button', { name: /Xem chi tiết/ })).toBeTruthy()
  })

  it('preserves the journey through details, seats and back', () => {
    render(<App />)
    openDetails()
    fireEvent.click(screen.getByRole('button', { name: /Xem tình trạng ghế/ }))
    expect(screen.getByRole('heading', { name: 'Tình trạng ghế theo chặng' })).toBeTruthy()
    expect(screen.getByLabelText('Ghế A1: Còn trống')).toBeTruthy()
    expect(screen.getByLabelText('Ghế A2: Không khả dụng')).toBeTruthy()
    expect(screen.getByText(/10:00 – 12:00/)).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: /← Quay lại/ }))
    expect(screen.getByRole('button', { name: /Xem tình trạng ghế/ })).toBeTruthy()
  })

  it('rejects identical stops', () => {
    render(<App />)
    fireEvent.change(screen.getByLabelText('Điểm trả'), { target: { value: '22222222-2222-4222-8222-222222222222' } })
    search()
    expect(screen.getByRole('alert').textContent).toContain('phải khác nhau')
    expect(screen.queryByRole('button', { name: /Xem chi tiết/ })).toBeNull()
  })

  it('rejects dates before the simulated day even on direct form submission', () => {
    render(<App />)
    const date = screen.getByLabelText<HTMLInputElement>('Ngày lên xe')
    fireEvent.change(date, { target: { value: '2026-11-19' } })
    expect(date.validity.rangeUnderflow).toBe(true)
    const form = date.closest('form')
    if (!form) throw new Error('Search form is missing')
    fireEvent.submit(form)
    expect(screen.getByRole('alert').textContent).toContain('quá khứ')
    expect(screen.queryByRole('button', { name: /Xem chi tiết/ })).toBeNull()
  })

  it('shows empty results for a date without sample trips', () => {
    render(<App />)
    fireEvent.change(screen.getByLabelText('Ngày lên xe'), { target: { value: '2026-11-22' } })
    search()
    expect(screen.getByRole('heading', { name: 'Không có chuyến phù hợp trong ví dụ này' })).toBeTruthy()
    expect(screen.queryByRole('button', { name: /Xem chi tiết/ })).toBeNull()
  })

  it('blocks closed trips and missing essential facts, but permits missing optional description', () => {
    render(<App />)
    openDetails()
    fireEvent.click(screen.getByRole('button', { name: 'Trip đã đóng' }))
    expect(screen.getByRole<HTMLButtonElement>('button', { name: /Xem tình trạng ghế/ }).disabled).toBe(true)
    fireEvent.click(screen.getByRole('button', { name: 'Thiếu dữ liệu thiết yếu' }))
    expect(screen.getByRole<HTMLButtonElement>('button', { name: /Xem tình trạng ghế/ }).disabled).toBe(true)
    fireEvent.click(screen.getByRole('button', { name: 'Thiếu mô tả tùy chọn' }))
    expect(screen.getByRole<HTMLButtonElement>('button', { name: /Xem tình trạng ghế/ }).disabled).toBe(false)
  })

  it('hides results on retrieval failure and permits sample retry', () => {
    render(<App />)
    search()
    fireEvent.click(screen.getByRole('button', { name: 'Lỗi không truy xuất được (503)' }))
    expect(screen.getByRole('alert').textContent).toContain('Không thể xác minh')
    expect(screen.queryByRole('button', { name: /Xem chi tiết/ })).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Thử lại ví dụ' }))
    expect(screen.getByRole('button', { name: /Xem chi tiết/ })).toBeTruthy()
  })

  it('never shows free seats when availability fails', () => {
    render(<App />)
    openDetails()
    fireEvent.click(screen.getByRole('button', { name: /Xem tình trạng ghế/ }))
    fireEvent.click(screen.getByRole('button', { name: 'Không tải được availability' }))
    expect(screen.queryByLabelText('Ghế A1: Còn trống')).toBeNull()
    expect(screen.getByRole('status').textContent).toContain('Không có ghế nào được xem là trống')
    fireEvent.click(screen.getByRole('button', { name: 'Thử tải lại' }))
    expect(screen.getByLabelText('Ghế A1: Còn trống')).toBeTruthy()
  })
})
