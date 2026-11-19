import { act, renderHook, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { journeyApi } from '../../shared/api/journey'
import { details, page, stops, trip } from '../../test/journeyFixtures'
import { useJourneyState, vietnamDate } from './useJourneyState'
import type { TripDetails, TripSearchPage } from '../../shared/api/journeyTypes'
const criteria = { fromStopId: stops[0].id, toStopId: stops[1].id, boardingDate: '2026-11-20' }
afterEach(() => vi.restoreAllMocks())
describe('journey request ownership', () => {
  it('keeps paging tied to submitted criteria rather than edited draft', async () => {
    vi.spyOn(journeyApi, 'listStops').mockResolvedValue(stops)
    const search = vi.spyOn(journeyApi, 'searchTrips').mockResolvedValue(page)
    const { result } = renderHook(useJourneyState)
    act(() => result.current.setDraftCriteria(criteria))
    act(() => result.current.submitSearch())
    await waitFor(() => expect(result.current.search.data).toBe(page))
    act(() => { result.current.setDraftCriteria({ ...criteria, boardingDate: '2026-11-21' }); result.current.changeSearchPage(1) })
    await waitFor(() => expect(search).toHaveBeenCalledTimes(2))
    expect(search.mock.calls[1][0]).toEqual(criteria)
    expect(search.mock.calls[1][1]).toEqual({ page: 1, size: 20 })
  })
  it('ignores a stale search response after newer criteria resolve', async () => {
    vi.spyOn(journeyApi, 'listStops').mockResolvedValue(stops)
    let oldResolve: ((data: TripSearchPage) => void) | undefined
    const search = vi.spyOn(journeyApi, 'searchTrips').mockImplementationOnce(() => new Promise(r => { oldResolve = r })).mockResolvedValue({ ...page, items: [] })
    const { result } = renderHook(useJourneyState)
    act(() => result.current.setDraftCriteria(criteria)); act(() => result.current.submitSearch())
    act(() => result.current.navigate('home'))
    act(() => result.current.setDraftCriteria({ ...criteria, boardingDate: '2026-11-21' })); act(() => result.current.submitSearch())
    await waitFor(() => expect(result.current.search.data?.items).toEqual([]))
    await act(async () => oldResolve?.(page))
    expect(result.current.search.data?.items).toEqual([])
    expect(search.mock.calls[0][2]?.aborted).toBe(true)
  })
  it('does not let an old details request overwrite a newly selected Trip', async () => {
    vi.spyOn(journeyApi, 'listStops').mockResolvedValue(stops)
    let oldResolve: ((data: TripDetails) => void) | undefined
    vi.spyOn(journeyApi, 'getTripDetails').mockImplementationOnce(() => new Promise(r => { oldResolve = r })).mockResolvedValue({ ...details, description: 'New details' })
    const { result } = renderHook(useJourneyState)
    act(() => result.current.selectTrip(trip))
    act(() => result.current.selectTrip({ ...trip, tripId: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa' }))
    await waitFor(() => expect(result.current.detail.data?.description).toBe('New details'))
    await act(async () => oldResolve?.(details))
    expect(result.current.detail.data?.description).toBe('New details')
  })
  it('uses the Vietnamese boarding date across UTC midnight', () => {
    expect(vietnamDate(new Date('2026-11-20T17:30:00Z'))).toBe('2026-11-21')
  })
})
