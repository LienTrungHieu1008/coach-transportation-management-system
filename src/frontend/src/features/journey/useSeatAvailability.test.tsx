import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { journeyApi, JourneyApiError } from '../../shared/api/journey'
import { availability } from '../../test/journeyFixtures'
import { useSeatAvailability } from './useSeatAvailability'
import type { SeatAvailability } from '../../shared/api/journeyTypes'
const selection = { tripId: availability.tripId, fromStopId: availability.fromStopId, toStopId: availability.toStopId }
const flush = () => act(async () => { await Promise.resolve() })
beforeEach(() => { vi.useFakeTimers(); vi.setSystemTime(new Date('2026-11-20T02:00:00Z')) })
afterEach(() => { vi.useRealTimers(); vi.restoreAllMocks() })
describe('seat refresh lifecycle', () => {
  it('polls every 15 seconds only while visible and refreshes on foreground', async () => {
    const get = vi.spyOn(journeyApi, 'getSeatAvailability').mockResolvedValue(availability)
    let visibility = 'visible'
    vi.spyOn(document, 'visibilityState', 'get').mockImplementation(() => visibility as DocumentVisibilityState)
    renderHook(() => useSeatAvailability(selection)); await flush()
    expect(get).toHaveBeenCalledTimes(1)
    await act(async () => vi.advanceTimersByTime(15000))
    expect(get).toHaveBeenCalledTimes(2)
    visibility = 'hidden'; act(() => document.dispatchEvent(new Event('visibilitychange')))
    await act(async () => vi.advanceTimersByTime(30000))
    expect(get).toHaveBeenCalledTimes(2)
    visibility = 'visible'; act(() => document.dispatchEvent(new Event('visibilitychange'))); await flush()
    expect(get).toHaveBeenCalledTimes(3)
  })
  it('uses server-relative expiry despite a two-hour client clock offset and never frees from timer', async () => {
    const expiring = { ...availability, seats: availability.seats.map(s => s.expiresAt ? { ...s, expiresAt: '2026-11-20T00:00:01Z' } : s) }
    let resolve: ((data: SeatAvailability) => void) | undefined
    vi.spyOn(journeyApi, 'getSeatAvailability').mockResolvedValueOnce(expiring).mockImplementation(() => new Promise(r => { resolve = r }))
    const { result } = renderHook(() => useSeatAvailability(selection)); await flush()
    await act(async () => vi.advanceTimersByTime(1000))
    expect(result.current.stale).toBe(true)
    expect(result.current.data?.seats[1].status).toBe('UNAVAILABLE')
    await act(async () => resolve?.({ ...availability, serverTime: '2026-11-20T00:00:01Z', seats: availability.seats.map(s => ({ ...s, status: 'FREE', expiresAt: null })) }))
    expect(result.current.stale).toBe(false)
    expect(result.current.data?.seats[1].status).toBe('FREE')
  })
  it('marks the previous map stale on refresh failure', async () => {
    vi.spyOn(journeyApi, 'getSeatAvailability').mockResolvedValueOnce(availability).mockRejectedValue(new Error('offline'))
    const { result } = renderHook(() => useSeatAvailability(selection)); await flush()
    act(() => result.current.refresh()); await flush()
    expect(result.current.stale).toBe(true)
    expect(result.current.error?.message).toBe('offline')
  })
  it('ignores old responses on Segment change and aborts the old request', async () => {
    let resolve: ((data: SeatAvailability) => void) | undefined
    const get = vi.spyOn(journeyApi, 'getSeatAvailability').mockImplementationOnce(() => new Promise(r => { resolve = r })).mockResolvedValue(availability)
    const { result, rerender } = renderHook(({ selected }) => useSeatAvailability(selected), { initialProps: { selected: selection } })
    const oldSignal = get.mock.calls[0][2]
    rerender({ selected: { ...selection, toStopId: '99999999-9999-4999-8999-999999999999' } }); await flush()
    await act(async () => resolve?.({ ...availability, seats: [] }))
    expect(oldSignal?.aborted).toBe(true)
    expect(result.current.data?.seats.length).toBe(2)
  })
  it('coalesces triggers while pending and cleans up on unmount', async () => {
    let resolve: ((data: SeatAvailability) => void) | undefined
    const get = vi.spyOn(journeyApi, 'getSeatAvailability').mockImplementationOnce(() => new Promise(r => { resolve = r })).mockResolvedValue(availability)
    const { result, unmount } = renderHook(() => useSeatAvailability(selection))
    act(() => { result.current.refresh(); result.current.refresh() })
    expect(get).toHaveBeenCalledTimes(1)
    await act(async () => resolve?.(availability)); await flush()
    expect(get).toHaveBeenCalledTimes(2)
    unmount(); await act(async () => vi.advanceTimersByTime(45000))
    expect(get).toHaveBeenCalledTimes(2)
  })
  it('refreshes at cutoff and exposes 409 as blocked without inventing FREE', async () => {
    vi.spyOn(journeyApi, 'getSeatAvailability').mockResolvedValueOnce({ ...availability, bookingClosesAt: '2026-11-20T00:00:01Z' })
      .mockRejectedValue(new JourneyApiError('Đã đóng bán', 409, { code: 'BOOKING_CLOSED', message: 'Đã đóng bán', fieldErrors: [] }))
    const { result } = renderHook(() => useSeatAvailability(selection)); await flush()
    await act(async () => vi.advanceTimersByTime(1000))
    expect(result.current.stale).toBe(true)
    expect(result.current.error).toMatchObject({ status: 409 })
  })
  it('clears cached seat data on non-retryable 404/422 so a later 503 cannot restore it', async () => {
    vi.spyOn(journeyApi, 'getSeatAvailability')
      .mockResolvedValueOnce(availability)
      .mockRejectedValueOnce(new JourneyApiError('Not found', 404, { code: 'TRIP_NOT_FOUND', message: 'Not found', fieldErrors: [] }))
      .mockRejectedValueOnce(new JourneyApiError('Unavailable', 503, { code: 'AVAILABILITY_UNAVAILABLE', message: 'Unavailable', fieldErrors: [] }))
    const { result } = renderHook(() => useSeatAvailability(selection)); await flush()
    expect(result.current.data).not.toBeNull()
    act(() => result.current.refresh()); await flush()
    expect(result.current.data).toBeNull()
    act(() => result.current.refresh()); await flush()
    expect(result.current.data).toBeNull()
    expect(result.current.error).toMatchObject({ status: 503 })
  })
})
