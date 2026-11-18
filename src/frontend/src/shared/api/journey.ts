import type {
  SearchCriteria,
  SeatAvailability,
  StopOption,
  TripDetails,
  TripSearchItem,
} from './journeyTypes'

type StopList = { items: StopOption[]; serverTime: string }
type TripSearchPage = {
  items: TripSearchItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  serverTime: string
}

export type ApiErrorBody = {
  code: string
  message: string
  fieldErrors: Array<{ field: string; message: string }>
}

export class JourneyApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly body: ApiErrorBody | null,
  ) {
    super(message)
    this.name = 'JourneyApiError'
  }
}

async function getJson<T>(path: string, signal?: AbortSignal): Promise<T> {
  const response = await fetch(`/api/v1${path}`, {
    method: 'GET',
    headers: { Accept: 'application/json' },
    cache: 'no-store',
    signal,
  })

  let body: unknown
  try {
    body = await response.json()
  } catch {
    body = null
  }

  if (!response.ok) {
    const errorBody = isApiErrorBody(body) ? body : null
    throw new JourneyApiError(
      errorBody?.message ?? 'Không thể tải dữ liệu chuyến xe.',
      response.status,
      errorBody,
    )
  }

  if (body === null || typeof body !== 'object') {
    throw new JourneyApiError('Dịch vụ trả về dữ liệu không hợp lệ.', response.status, null)
  }
  return body as T
}

function isApiErrorBody(value: unknown): value is ApiErrorBody {
  if (!value || typeof value !== 'object') return false
  const candidate = value as Partial<ApiErrorBody>
  return typeof candidate.code === 'string'
    && typeof candidate.message === 'string'
    && Array.isArray(candidate.fieldErrors)
}

export const journeyApi = {
  async listStops(signal?: AbortSignal): Promise<StopOption[]> {
    return (await getJson<StopList>('/stops', signal)).items
  },

  async searchTrips(criteria: SearchCriteria, signal?: AbortSignal): Promise<TripSearchItem[]> {
    const query = new URLSearchParams({
      fromStopId: criteria.fromStopId,
      toStopId: criteria.toStopId,
      boardingDate: criteria.boardingDate,
      page: '0',
      size: '20',
    })
    return (await getJson<TripSearchPage>(`/trips?${query.toString()}`, signal)).items
  },

  getTripDetails(
    tripId: string,
    criteria: SearchCriteria,
    signal?: AbortSignal,
  ): Promise<TripDetails> {
    const query = new URLSearchParams({
      fromStopId: criteria.fromStopId,
      toStopId: criteria.toStopId,
    })
    return getJson<TripDetails>(`/trips/${encodeURIComponent(tripId)}?${query.toString()}`, signal)
  },

  getSeatAvailability(
    tripId: string,
    criteria: SearchCriteria,
    signal?: AbortSignal,
  ): Promise<SeatAvailability> {
    const query = new URLSearchParams({
      fromStopId: criteria.fromStopId,
      toStopId: criteria.toStopId,
    })
    return getJson<SeatAvailability>(
      `/trips/${encodeURIComponent(tripId)}/seat-availability?${query.toString()}`,
      signal,
    )
  },
}
