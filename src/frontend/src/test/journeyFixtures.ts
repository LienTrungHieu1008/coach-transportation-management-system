import type { SeatAvailability, TripDetails, TripSearchPage } from '../shared/api/journeyTypes'
import { demoAvailability, demoDetails, demoStops, demoTrip } from '../features/journey/demoData'
export const stops = [...demoStops, { id: '99999999-9999-4999-8999-999999999999', name: 'Demo Stop C' }]
export const trip = { ...demoTrip, fareAmountVnd: 100000 }
export const details: TripDetails = { ...demoDetails, selectedSegment: { ...demoDetails.selectedSegment!, fareAmountVnd: 100000 } }
export const availability: SeatAvailability = structuredClone(demoAvailability)
export const page: TripSearchPage = { items: [trip], page: 0, size: 20, totalElements: 1, totalPages: 1, serverTime: details.serverTime }
export function response(body: unknown, status = 200) {
  return { ok: status < 400, status, json: async () => structuredClone(body) }
}
