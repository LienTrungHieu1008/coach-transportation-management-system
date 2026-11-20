import { act, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { journeyApi, JourneyApiError } from '../../shared/api/journey'
import type { SeatAvailability } from '../../shared/api/journeyTypes'
import { availability } from '../../test/journeyFixtures'
import { SeatsPage } from './JourneyPages'

const selection = { tripId: availability.tripId, fromStopId: availability.fromStopId, toStopId: availability.toStopId }
const notBookable = new JourneyApiError('Chuyến xe đã đóng bán', 409, { code: 'TRIP_NOT_BOOKABLE', message: 'Chuyến xe đã đóng bán', fieldErrors: [] })
const flush = () => act(async () => { await Promise.resolve(); await Promise.resolve() })
const seatImages = () => screen.queryAllByRole('img', { name: /^Ghế / })
const open = { ...availability, bookingClosesAt: '2026-11-21T01:30:00Z', seats: availability.seats.map(s => ({ ...s, expiresAt: null })) }

beforeEach(() => {
  vi.useFakeTimers(); vi.setSystemTime(new Date('2026-11-20T02:00:00Z'))
  vi.spyOn(document, 'visibilityState', 'get').mockReturnValue('visible')
})
afterEach(() => { vi.useRealTimers(); vi.restoreAllMocks() })

describe('B02: seat page after a non-retryable rejection', () => {
  it('keeps the seat map hidden while the next poll is in flight', async () => {
    vi.spyOn(journeyApi, 'getSeatAvailability')
      .mockResolvedValueOnce({ ...open, serverTime: '2026-11-20T02:00:00Z' })
      .mockRejectedValueOnce(notBookable)
      .mockImplementation(() => new Promise<SeatAvailability>(() => {}))
    render(<SeatsPage selection={selection} onNavigate={() => {}}/>); await flush()
    expect(seatImages()).toHaveLength(2)

    await act(async () => vi.advanceTimersByTime(15000)); await flush()
    expect(screen.getByRole('heading', { name: 'Chuyến không còn mở bán' })).toBeTruthy()
    expect(seatImages()).toHaveLength(0)

    await act(async () => vi.advanceTimersByTime(15000)); await flush()
    expect(seatImages()).toHaveLength(0)
    expect(screen.getByRole('heading', { name: 'Chuyến không còn mở bán' })).toBeTruthy()
    expect(screen.queryByText(/Đang xác minh lại/)).toBeNull()
  })
  it('shows the map again once a later poll succeeds', async () => {
    vi.spyOn(journeyApi, 'getSeatAvailability')
      .mockRejectedValueOnce(notBookable)
      .mockResolvedValue({ ...open, serverTime: '2026-11-20T02:00:15Z' })
    render(<SeatsPage selection={selection} onNavigate={() => {}}/>); await flush()
    expect(seatImages()).toHaveLength(0)
    await act(async () => vi.advanceTimersByTime(15000)); await flush()
    expect(seatImages()).toHaveLength(2)
    expect(screen.queryByRole('alert')).toBeNull()
  })
})
