import { useEffect, useRef, useState } from 'react'
import { isAbort, journeyApi } from '../../shared/api/journey'
import type { SeatAvailability, SegmentSelection } from '../../shared/api/journeyTypes'
import { describeError } from './journeyErrors'

export type SeatSelection = SegmentSelection & { tripId: string }
type SeatState = { key: string; data: SeatAvailability | null; loading: boolean; error: Error | null; stale: boolean }
const blank = (key: string): SeatState => ({ key, data: null, loading: false, error: null, stale: true })
export function useSeatAvailability(selection: SeatSelection | null) {
  const key = selection ? `${selection.tripId}/${selection.fromStopId}/${selection.toStopId}` : ''
  const [state, setState] = useState<SeatState>(() => blank(''))
  const refreshRef = useRef<() => void>(() => {})
  const tripId = selection?.tripId, fromStopId = selection?.fromStopId, toStopId = selection?.toStopId
  useEffect(() => {
    if (!tripId || !fromStopId || !toStopId) { refreshRef.current = () => {}; return }
    let active = true, pending = false, queued = false
    let controller: AbortController | null = null
    let boundary: ReturnType<typeof setTimeout> | undefined
    let timeout: ReturnType<typeof setTimeout> | undefined
    async function refresh() {
      if (!active) return
      if (pending) { queued = true; return }
      pending = true
      let requestActive = true
      const started = performance.now()
      controller = new AbortController()
      // Keep the last error until this request settles: clearing it here would re-show the previous
      // seat map during every poll after a 409 (B02). Success clears it; a failure replaces it.
      setState(old => (old.key === key ? { ...old, loading: true } : { ...blank(key), loading: true }))
      timeout = setTimeout(() => {
        if (!active || !requestActive) return
        requestActive = false; pending = false; queued = false; controller?.abort()
        clearTimeout(boundary)
        setState(old => ({ ...old, loading: false, stale: true, error: new Error('Dịch vụ phản hồi quá lâu. Hãy thử lại.') }))
      }, 15000)
      try {
        const data = await journeyApi.getSeatAvailability(tripId!, { fromStopId: fromStopId!, toStopId: toStopId! }, controller.signal)
        if (!active || !requestActive) return
        clearTimeout(boundary)
        const server = Date.parse(data.serverTime)
        const until = [Date.parse(data.bookingClosesAt), ...data.seats.flatMap(s => s.expiresAt ? [Date.parse(s.expiresAt)] : [])]
          .map(t => t - server - (performance.now() - started))
        const next = Math.min(...until)
        const stale = next <= 0
        setState(old => ({
          key, data: stale ? old.data : data, loading: false, stale,
          error: stale ? new Error('Dữ liệu ghế đã quá hạn xác minh. Hãy tải lại.') : null,
        }))
        if (!stale) boundary = setTimeout(() => {
          if (!active) return
          setState(old => ({ ...old, stale: true }))
          if (document.visibilityState === 'visible') void refresh()
        }, Math.min(next, 2147483647))
      } catch (error) {
        if (active && requestActive && !isAbort(error)) {
          clearTimeout(boundary)
          const normalized = error instanceof Error ? error : new Error('Không thể tải ghế. Hãy thử lại.')
          // Any non-retryable rejection (409, 404, 400, 422) invalidates the cached map.
          const rejected = describeError(normalized).action !== 'retry'
          setState(old => ({
            ...old, data: rejected ? null : old.data, loading: false, stale: true,
            error: normalized,
          }))
        }
      } finally {
        if (requestActive) {
          requestActive = false; clearTimeout(timeout); pending = false
          if (active && queued) { queued = false; void refresh() }
        }
      }
    }
    refreshRef.current = () => { void refresh() }
    const poll = setInterval(() => { if (document.visibilityState === 'visible') void refresh() }, 15000)
    const foreground = () => {
      if (document.visibilityState === 'visible') {
        setState(old => ({ ...old, stale: true })); void refresh()
      }
    }
    document.addEventListener('visibilitychange', foreground)
    void refresh()
    return () => {
      active = false; controller?.abort(); clearTimeout(timeout); clearTimeout(boundary); clearInterval(poll)
      document.removeEventListener('visibilitychange', foreground)
    }
  }, [key, tripId, fromStopId, toStopId])
  // Never render a preceding selection during the render before effect cleanup/setup.
  const current = state.key === key ? state : { ...blank(key), loading: Boolean(key) }
  return { ...current, refresh: () => refreshRef.current() }
}
