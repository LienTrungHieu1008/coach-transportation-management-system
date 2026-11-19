import type { NamedReference, SeatAvailability, TripDetails, TripSearchItem, TripSearchPage } from './journeyTypes'
type ObjectValue = Record<string, unknown>
export const object = (v: unknown): v is ObjectValue => v !== null && typeof v === 'object' && !Array.isArray(v)
const text = (v: unknown): v is string => typeof v === 'string' && v.trim().length > 0
const integer = (v: unknown): v is number => typeof v === 'number' && Number.isSafeInteger(v) && v >= 0
const uuid = (v: unknown): v is string => typeof v === 'string' && /^[\da-f]{8}-[\da-f]{4}-[\da-f]{4}-[\da-f]{4}-[\da-f]{12}$/i.test(v)
const instant = (v: unknown): v is string => typeof v === 'string' && /^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d(?:\.\d{1,9})?Z$/.test(v) && Number.isFinite(Date.parse(v))
const nullable = (v: unknown, guard: (v: unknown) => boolean) => v === null || guard(v)
const unique = (v: unknown[]) => new Set(v).size === v.length
export const reference = (v: unknown): v is NamedReference => object(v) && uuid(v.id) && text(v.name)
export function stopList(v: unknown): v is { items: NamedReference[]; serverTime: string } {
  return object(v) && instant(v.serverTime) && Array.isArray(v.items) && v.items.every(reference) && unique(v.items.map(s => s.id))
}
function searchItem(v: unknown): v is TripSearchItem {
  return object(v) && uuid(v.tripId) && reference(v.route) && reference(v.fromStop) && reference(v.toStop) && v.fromStop.id !== v.toStop.id
    && instant(v.boardingTime) && instant(v.alightingTime) && instant(v.bookingClosesAt) && text(v.vehicleType) && integer(v.fareAmountVnd)
}
export function searchPage(v: unknown): v is TripSearchPage {
  return object(v) && Array.isArray(v.items) && v.items.every(searchItem) && unique(v.items.map(i => i.tripId))
    && integer(v.page) && integer(v.size) && v.size > 0 && v.size <= 100 && integer(v.totalElements) && integer(v.totalPages) && instant(v.serverTime)
    && v.items.length <= v.size && v.totalPages === Math.ceil(v.totalElements / v.size)
}
const reasonNames = ['TRIP_NOT_SCHEDULED', 'BOOKING_CLOSED', 'ESSENTIAL_DATA_INCOMPLETE']
export function tripDetails(v: unknown): v is TripDetails {
  if (!object(v) || !uuid(v.tripId) || typeof v.status !== 'string' || !['Scheduled', 'Departed', 'Completed', 'Cancelled'].includes(v.status)
    || !nullable(v.route, reference) || !nullable(v.vehicle, c => object(c) && uuid(c.id) && text(c.vehicleType) && nullable(c.seatLayoutId, uuid))
    || !nullable(v.description, x => typeof x === 'string') || !instant(v.serverTime) || !nullable(v.bookingClosesAt, instant)
    || !Array.isArray(v.stops) || !v.stops.every(s => object(s) && reference(s.stop) && integer(s.index) && nullable(s.plannedArrivalTime, instant) && nullable(s.plannedDepartureTime, instant))
    || !Array.isArray(v.legFares) || !v.legFares.every(f => object(f) && uuid(f.fromStopId) && uuid(f.toStopId) && integer(f.fareAmountVnd))) return false
  const e = v.bookingEligibility
  if (!object(e) || typeof e.allowed !== 'boolean' || !Array.isArray(e.reasons) || !e.reasons.every(r => typeof r === 'string' && reasonNames.includes(r))
    || !unique(e.reasons) || e.allowed !== (e.reasons.length === 0)) return false
  return nullable(v.selectedSegment, s => object(s) && uuid(s.fromStopId) && uuid(s.toStopId) && s.fromStopId !== s.toStopId
    && nullable(s.boardingTime, instant) && nullable(s.alightingTime, instant) && nullable(s.fareAmountVnd, integer))
}
export function seatAvailability(v: unknown): v is SeatAvailability {
  if (!object(v) || !uuid(v.tripId) || !uuid(v.fromStopId) || !uuid(v.toStopId) || v.fromStopId === v.toStopId
    || !instant(v.serverTime) || !instant(v.bookingClosesAt) || !object(v.layout) || !uuid(v.layout.id)
    || !Array.isArray(v.layout.seats) || v.layout.seats.length === 0 || !Array.isArray(v.seats)) return false
  const positions: string[] = [], ids: string[] = []
  for (const s of v.layout.seats) {
    if (!object(s) || !uuid(s.physicalSeatId) || !text(s.label) || !integer(s.deck) || !integer(s.row) || !integer(s.column)) return false
    ids.push(s.physicalSeatId); positions.push(`${s.deck}/${s.row}/${s.column}`)
  }
  if (!unique(ids) || !unique(positions) || v.seats.length !== ids.length) return false
  const states: string[] = []
  for (const s of v.seats) {
    if (!object(s) || !uuid(s.physicalSeatId) || !ids.includes(s.physicalSeatId) || typeof s.status !== 'string' || !['FREE', 'UNAVAILABLE', 'UNKNOWN'].includes(s.status)
      || !nullable(s.expiresAt, instant) || (s.status !== 'UNAVAILABLE' && s.expiresAt !== null)) return false
    states.push(s.physicalSeatId)
  }
  return unique(states)
}
