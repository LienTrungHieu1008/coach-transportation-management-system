import type { SeatAvailability, StopOption, TripDetails, TripSearchItem } from '../../shared/api/journeyTypes'

// These fixed snapshots mirror docs/contracts/examples for UI development only.
// They are not API responses from a running backend or seeded demo database.
export const demoStops: StopOption[] = [
  { id: '22222222-2222-4222-8222-222222222222', name: 'Demo Stop A' },
  { id: '33333333-3333-4333-8333-333333333333', name: 'Demo Stop B' },
]

export const demoTrip: TripSearchItem = {
  tripId: '11111111-1111-4111-8111-111111111111',
  route: { id: '44444444-4444-4444-8444-444444444444', name: 'Demo Route A-B' },
  fromStop: demoStops[0],
  toStop: demoStops[1],
  boardingTime: '2026-11-20T03:00:00Z',
  alightingTime: '2026-11-20T05:00:00Z',
  vehicleType: 'Demo coach',
  fareAmountVnd: 120000,
  bookingClosesAt: '2026-11-20T02:30:00Z',
}

export const demoDetails: TripDetails = {
  tripId: demoTrip.tripId,
  status: 'Scheduled',
  route: demoTrip.route,
  vehicle: {
    id: '66666666-6666-4666-8666-666666666666',
    vehicleType: 'Demo coach',
    seatLayoutId: '55555555-5555-4555-8555-555555555555',
  },
  stops: [
    { stop: demoStops[0], index: 0, plannedArrivalTime: '2026-11-20T03:00:00Z', plannedDepartureTime: '2026-11-20T03:00:00Z' },
    { stop: demoStops[1], index: 1, plannedArrivalTime: '2026-11-20T05:00:00Z', plannedDepartureTime: '2026-11-20T05:00:00Z' },
  ],
  legFares: [{ fromStopId: demoStops[0].id, toStopId: demoStops[1].id, fareAmountVnd: 120000 }],
  description: null,
  selectedSegment: {
    fromStopId: demoStops[0].id,
    toStopId: demoStops[1].id,
    boardingTime: '2026-11-20T03:00:00Z',
    alightingTime: '2026-11-20T05:00:00Z',
    fareAmountVnd: 120000,
  },
  bookingClosesAt: '2026-11-20T02:30:00Z',
  bookingEligibility: { allowed: true, reasons: [] },
  serverTime: '2026-11-20T00:00:00Z',
}

export const demoAvailability: SeatAvailability = {
  tripId: demoTrip.tripId,
  fromStopId: demoStops[0].id,
  toStopId: demoStops[1].id,
  serverTime: '2026-11-20T00:00:00Z',
  bookingClosesAt: '2026-11-20T02:30:00Z',
  layout: {
    id: '55555555-5555-4555-8555-555555555555',
    seats: [
      { physicalSeatId: '77777777-7777-4777-8777-777777777777', label: 'A1', deck: 0, row: 0, column: 0 },
      { physicalSeatId: '88888888-8888-4888-8888-888888888888', label: 'A2', deck: 0, row: 0, column: 2 },
    ],
  },
  seats: [
    { physicalSeatId: '77777777-7777-4777-8777-777777777777', status: 'FREE', expiresAt: null },
    { physicalSeatId: '88888888-8888-4888-8888-888888888888', status: 'UNAVAILABLE', expiresAt: '2026-11-20T00:05:00Z' },
  ],
}

export const demoBoardingDate = '2026-11-20'
