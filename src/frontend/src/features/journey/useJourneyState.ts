import { useState } from 'react'
import type { JourneyPage } from './JourneyPages'
import type { SearchCriteria, TripSearchItem } from '../../shared/api/journeyTypes'
import { demoBoardingDate, demoStops, demoTrip } from './demoData'

export function useJourneyState() {
  const [page, setPage] = useState<JourneyPage>('home')
  const [criteria, setCriteria] = useState<SearchCriteria>({ fromStopId: demoStops[0].id, toStopId: demoStops[1].id, boardingDate: demoBoardingDate })
  const [trip, setTrip] = useState<TripSearchItem>(demoTrip)
  return { page, setPage, criteria, setCriteria, trip, setTrip }
}
