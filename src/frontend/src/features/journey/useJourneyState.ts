import { useEffect, useState } from 'react'
import { isAbort, journeyApi } from '../../shared/api/journey'
import type { SearchCriteria, StopOption, TripDetails, TripSearchItem, TripSearchPage } from '../../shared/api/journeyTypes'

export type JourneyPage = 'home' | 'results' | 'details' | 'seats'
export type LoadState<T> = { data: T | null; loading: boolean; error: Error | null }
const initial = <T,>(): LoadState<T> => ({ data: null, loading: false, error: null })
const waiting = <T,>(): LoadState<T> => ({ data: null, loading: true, error: null })
export function vietnamDate(date = new Date()) {
  const parts = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Ho_Chi_Minh', year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(date)
  return ['year', 'month', 'day'].map(type => parts.find(p => p.type === type)?.value).join('-')
}
const asError = (e: unknown) => e instanceof Error ? e : new Error('Không thể kết nối dịch vụ. Hãy thử lại.')
export function useJourneyState() {
  const [page, setPage] = useState<JourneyPage>('home')
  const [draftCriteria, setDraftCriteria] = useState<SearchCriteria>(() => ({ fromStopId: '', toStopId: '', boardingDate: vietnamDate() }))
  const [submittedCriteria, setSubmittedCriteria] = useState<SearchCriteria | null>(null)
  const [catalogue, setCatalogue] = useState<LoadState<StopOption[]>>(waiting)
  const [search, setSearch] = useState<LoadState<TripSearchPage>>(initial)
  const [detail, setDetail] = useState<LoadState<TripDetails>>(initial)
  const [selectedTrip, setSelectedTrip] = useState<TripSearchItem | null>(null)
  const [searchIndex, setSearchIndex] = useState(0)
  const [retries, setRetries] = useState({ catalogue: 0, search: 0, detail: 0 })
  useEffect(() => {
    const controller = new AbortController()
    let active = true
    journeyApi.listStops(controller.signal).then(data => {
      if (active) setCatalogue({ data, loading: false, error: null })
    }).catch(e => { if (active && !isAbort(e)) setCatalogue({ data: null, loading: false, error: asError(e) }) })
    return () => { active = false; controller.abort() }
  }, [retries.catalogue])
  useEffect(() => {
    if (page !== 'results' || !submittedCriteria) return
    const controller = new AbortController()
    let active = true
    journeyApi.searchTrips(submittedCriteria, { page: searchIndex, size: 20 }, controller.signal).then(data => {
      if (active) setSearch({ data, loading: false, error: null })
    }).catch(e => { if (active && !isAbort(e)) setSearch({ data: null, loading: false, error: asError(e) }) })
    return () => { active = false; controller.abort() }
  }, [page, submittedCriteria, searchIndex, retries.search])
  useEffect(() => {
    if (page !== 'details' || !selectedTrip) return
    const controller = new AbortController()
    let active = true
    journeyApi.getTripDetails(selectedTrip.tripId, { fromStopId: selectedTrip.fromStop.id, toStopId: selectedTrip.toStop.id }, controller.signal).then(data => {
      if (active) setDetail({ data, loading: false, error: null })
    }).catch(e => { if (active && !isAbort(e)) setDetail({ data: null, loading: false, error: asError(e) }) })
    return () => { active = false; controller.abort() }
  }, [page, selectedTrip, retries.detail])
  function submitSearch() {
    setSubmittedCriteria({ ...draftCriteria }); setSearchIndex(0); setSelectedTrip(null)
    setSearch(waiting()); setDetail(initial()); setPage('results')
  }
  function selectTrip(trip: TripSearchItem) { setSelectedTrip(trip); setDetail(waiting()); setPage('details') }
  function retry(target: keyof typeof retries) {
    if (target === 'catalogue') setCatalogue(waiting())
    if (target === 'search') setSearch(waiting())
    if (target === 'detail') setDetail(waiting())
    setRetries(old => ({ ...old, [target]: old[target] + 1 }))
  }
  function navigate(next: JourneyPage) {
    if (next === 'results' && page !== next) setSearch(waiting())
    if (next === 'details' && page !== next) setDetail(waiting())
    setPage(next)
  }
  function changeSearchPage(index: number) { if (index !== searchIndex) { setSearch(waiting()); setSearchIndex(index) } }
  function openSeats() {
    if (detail.data?.bookingEligibility.allowed && detail.data.selectedSegment && selectedTrip
      && detail.data.selectedSegment.fareAmountVnd !== null) setPage('seats')
  }
  return { page, navigate, draftCriteria, setDraftCriteria, submittedCriteria, catalogue, search, detail, selectedTrip,
    submitSearch, selectTrip, changeSearchPage, retry, openSeats }
}
