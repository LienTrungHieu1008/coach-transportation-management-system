import { describe, expect, it, vi } from 'vitest'
import { journeyApi, JourneyApiError } from './journey'
import { demoAvailability, demoDetails, demoStops, demoTrip } from '../../features/journey/demoData'

const criteria = { fromStopId: demoStops[0].id, toStopId: demoStops[1].id, boardingDate: '2026-11-20' }
function reply(body: unknown, status = 200) {
  const fetcher = vi.fn().mockResolvedValue({ ok: status < 400, status, json: async () => body })
  vi.stubGlobal('fetch', fetcher)
  return fetcher
}
describe('journey API contract', () => {
  it.each([{}, { items: [{}] }, [], null])('rejects malformed catalogue %j', async (body) => {
    reply(body)
    await expect(journeyApi.listStops()).rejects.toBeInstanceOf(JourneyApiError)
  })
  it('returns verified catalogue', async () => {
    reply({ items: demoStops, serverTime: demoDetails.serverTime })
    expect(await journeyApi.listStops()).toEqual(demoStops)
  })
  it('preserves paging and sends submitted criteria', async () => {
    const page = { items: [demoTrip], page: 1, size: 20, totalElements: 21, totalPages: 2, serverTime: demoDetails.serverTime }
    const fetcher = reply(page)
    expect(await journeyApi.searchTrips(criteria, { page: 1, size: 20 })).toEqual(page)
    const url = new URL(fetcher.mock.calls[0][0], 'http://localhost')
    expect(url.searchParams.get('page')).toBe('1')
    expect(url.searchParams.get('fromStopId')).toBe(criteria.fromStopId)
  })
  it('accepts partial details and an optional absent Segment', async () => {
    reply({ ...demoDetails, route: null, vehicle: null, selectedSegment: null,
      bookingEligibility: { allowed: false, reasons: ['ESSENTIAL_DATA_INCOMPLETE'] } })
    expect((await journeyApi.getTripDetails(demoTrip.tripId)).selectedSegment).toBeNull()
  })
  it('rejects details for another Trip', async () => {
    reply({ ...demoDetails, tripId: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa' })
    await expect(journeyApi.getTripDetails(demoTrip.tripId, criteria)).rejects.toBeInstanceOf(JourneyApiError)
  })
  it('rejects availability for a different Segment', async () => {
    reply({ ...demoAvailability, fromStopId: criteria.toStopId, toStopId: criteria.fromStopId })
    await expect(journeyApi.getSeatAvailability(demoTrip.tripId, criteria)).rejects.toBeInstanceOf(JourneyApiError)
  })
  it('rejects a map with missing statuses or duplicate positions', async () => {
    reply({ ...demoAvailability, seats: [] })
    await expect(journeyApi.getSeatAvailability(demoTrip.tripId, criteria)).rejects.toBeInstanceOf(JourneyApiError)
    reply({ ...demoAvailability, layout: { ...demoAvailability.layout,
      seats: demoAvailability.layout.seats.map(seat => ({ ...seat, row: 0, column: 0 })) } })
    await expect(journeyApi.getSeatAvailability(demoTrip.tripId, criteria)).rejects.toBeInstanceOf(JourneyApiError)
  })
  it('rejects enum values encoded as arrays rather than strings', async () => {
    reply({ ...demoAvailability, seats: demoAvailability.seats.map(s => ({ ...s, status: ['FREE'], expiresAt: null })) })
    await expect(journeyApi.getSeatAvailability(demoTrip.tripId, criteria)).rejects.toBeInstanceOf(JourneyApiError)
    reply({ ...demoDetails, status: ['Scheduled'] })
    await expect(journeyApi.getTripDetails(demoTrip.tripId, criteria)).rejects.toBeInstanceOf(JourneyApiError)
    reply({ ...demoDetails, bookingEligibility: { allowed: false, reasons: [['ESSENTIAL_DATA_INCOMPLETE']] } })
    await expect(journeyApi.getTripDetails(demoTrip.tripId, criteria)).rejects.toBeInstanceOf(JourneyApiError)
  })
  it('preserves AbortError from JSON consumption', async () => {
    const abort = new DOMException('Cancelled', 'AbortError')
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, status: 200, json: async () => { throw abort } }))
    await expect(journeyApi.listStops()).rejects.toBe(abort)
  })
  it('maps HTTP errors and discards invalid field errors', async () => {
    reply({ code: 'INVALID_SEGMENT', message: 'Invalid segment', fieldErrors: [42] }, 422)
    await expect(journeyApi.listStops()).rejects.toMatchObject({ status: 422, body: null })
    reply({ code: 'TRIP_DATA_UNAVAILABLE', message: 'Retry', fieldErrors: [] }, 503)
    await expect(journeyApi.listStops()).rejects.toMatchObject({ status: 503, body: { code: 'TRIP_DATA_UNAVAILABLE' } })
  })
})
