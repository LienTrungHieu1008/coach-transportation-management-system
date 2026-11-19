export type NamedReference = { id: string; name: string }

export type StopOption = NamedReference

export type TripSearchItem = {
  tripId: string
  route: NamedReference
  fromStop: NamedReference
  toStop: NamedReference
  boardingTime: string
  alightingTime: string
  vehicleType: string
  fareAmountVnd: number
  bookingClosesAt: string
}

export type TripStop = {
  stop: NamedReference
  index: number
  plannedArrivalTime: string | null
  plannedDepartureTime: string | null
}

export type TripDetails = {
  tripId: string
  status: 'Scheduled' | 'Departed' | 'Completed' | 'Cancelled'
  route: NamedReference | null
  vehicle: { id: string; vehicleType: string; seatLayoutId: string | null } | null
  stops: TripStop[]
  legFares: Array<{ fromStopId: string; toStopId: string; fareAmountVnd: number }>
  description: string | null
  selectedSegment: {
    fromStopId: string
    toStopId: string
    boardingTime: string | null
    alightingTime: string | null
    fareAmountVnd: number | null
  } | null
  bookingClosesAt: string | null
  bookingEligibility: { allowed: boolean; reasons: string[] }
  serverTime: string
}

export type SeatAvailability = {
  tripId: string
  fromStopId: string
  toStopId: string
  serverTime: string
  bookingClosesAt: string
  layout: {
    id: string
    seats: Array<{ physicalSeatId: string; label: string; deck: number; row: number; column: number }>
  }
  seats: Array<{ physicalSeatId: string; status: 'FREE' | 'UNAVAILABLE' | 'UNKNOWN'; expiresAt: string | null }>
}

export type SearchCriteria = {
  fromStopId: string
  toStopId: string
  boardingDate: string
}

export type SegmentSelection = Pick<SearchCriteria, 'fromStopId' | 'toStopId'>
export type TripSearchPage = { items: TripSearchItem[]; page: number; size: number; totalElements: number; totalPages: number; serverTime: string }
