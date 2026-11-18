// Client started by Hoàng in PR #17; completed for F2–F4 integration.
import type { SearchCriteria, SeatAvailability, SegmentSelection, StopOption, TripDetails, TripSearchPage } from './journeyTypes'
import { object, searchPage, seatAvailability, stopList, tripDetails } from './journeyValidation'
export type ApiErrorBody = { code: string; message: string; fieldErrors: Array<{ field: string; message: string }> }
export class JourneyApiError extends Error {
  readonly status: number
  readonly body: ApiErrorBody | null
  constructor(message: string, status: number, body: ApiErrorBody | null) {
    super(message)
    this.name = 'JourneyApiError'
    this.status = status
    this.body = body
  }
}
export function isAbort(error: unknown) { return object(error) && error.name === 'AbortError' }
function apiError(v: unknown): v is ApiErrorBody {
  return object(v) && typeof v.code === 'string' && typeof v.message === 'string' && Array.isArray(v.fieldErrors)
    && v.fieldErrors.every(f => object(f) && typeof f.field === 'string' && typeof f.message === 'string')
}
function invalid(status = 200): never { throw new JourneyApiError('Dịch vụ trả về dữ liệu không hợp lệ. Hãy thử lại.', status, null) }
async function getJson<T>(path: string, guard: (value: unknown) => value is T, signal?: AbortSignal): Promise<T> {
  const response = await fetch(`/api/v1${path}`, { method: 'GET', credentials: 'same-origin', headers: { Accept: 'application/json' }, cache: 'no-store', signal })
  let body: unknown
  try { body = await response.json() } catch (error) {
    if (isAbort(error)) throw error
    body = null
  }
  if (!response.ok) {
    const errorBody = apiError(body) ? body : null
    throw new JourneyApiError(errorBody?.message ?? 'Không thể tải dữ liệu chuyến xe.', response.status, errorBody)
  }
  if (!guard(body)) invalid(response.status)
  return body
}
function samePair(actual: SegmentSelection, expected: SegmentSelection) {
  return actual.fromStopId === expected.fromStopId && actual.toStopId === expected.toStopId
}
export const journeyApi = {
  async listStops(signal?: AbortSignal): Promise<StopOption[]> {
    return (await getJson('/stops', stopList, signal)).items
  },
  async searchTrips(criteria: SearchCriteria, options = { page: 0, size: 20 }, signal?: AbortSignal): Promise<TripSearchPage> {
    const query = new URLSearchParams({ ...criteria, page: String(options.page), size: String(options.size) })
    const result = await getJson(`/trips?${query}`, searchPage, signal)
    if (result.page !== options.page || result.size !== options.size || !result.items.every(i => i.fromStop.id === criteria.fromStopId && i.toStop.id === criteria.toStopId)) invalid()
    return result
  },
  async getTripDetails(tripId: string, segment?: SegmentSelection, signal?: AbortSignal): Promise<TripDetails> {
    const query = segment ? `?${new URLSearchParams(segment)}` : ''
    const result = await getJson(`/trips/${encodeURIComponent(tripId)}${query}`, tripDetails, signal)
    if (result.tripId !== tripId || (segment && result.selectedSegment && !samePair(result.selectedSegment, segment)) || (!segment && result.selectedSegment !== null)) invalid()
    return result
  },
  async getSeatAvailability(tripId: string, segment: SegmentSelection, signal?: AbortSignal): Promise<SeatAvailability> {
    const result = await getJson(`/trips/${encodeURIComponent(tripId)}/seat-availability?${new URLSearchParams(segment)}`, seatAvailability, signal)
    if (result.tripId !== tripId || !samePair(result, segment)) invalid()
    return result
  },
}
