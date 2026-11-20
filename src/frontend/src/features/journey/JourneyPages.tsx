import { useState, type FormEvent } from 'react'
import type { SearchCriteria, StopOption, TripDetails, TripSearchItem, TripSearchPage } from '../../shared/api/journeyTypes'
import { JourneyIcon } from './JourneyIcon'
import { describeError } from './journeyErrors'
import { useSeatAvailability, type SeatSelection } from './useSeatAvailability'
import { vietnamDate, type JourneyPage, type LoadState } from './useJourneyState'

const zone = 'Asia/Ho_Chi_Minh'
const money = (n: number | null) => n === null ? 'Chưa có giá' : `${new Intl.NumberFormat('vi-VN').format(n)}đ`
const time = (s: string | null) => s === null ? 'Chưa có giờ' : new Intl.DateTimeFormat('vi-VN', { timeZone: zone, hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).format(new Date(s))
const fullTime = (s: string) => new Intl.DateTimeFormat('vi-VN', { timeZone: zone, dateStyle: 'short', timeStyle: 'medium' }).format(new Date(s))
const reasonLabels: Record<string, string> = { TRIP_NOT_SCHEDULED: 'Chuyến không còn mở bán.', BOOKING_CLOSED: 'Đã qua giờ đóng bán chung của chuyến.', ESSENTIAL_DATA_INCOMPLETE: 'Thông tin thiết yếu chưa đủ hoặc không còn hoạt động; không thể tiếp tục.' }
type Navigation = { onNavigate: (page: JourneyPage) => void }
function JourneyProgress({ current, onNavigate }: Navigation & { current: number }) {
  const steps = [{ label: 'Tìm chuyến', page: 'results' as const }, { label: 'Chi tiết chuyến', page: 'details' as const }, { label: 'Tình trạng ghế', page: 'seats' as const }]
  return <nav className="journey-progress" aria-label="Tiến trình tra cứu"><ol>{steps.map((step, i) => <li key={step.page} className={i === current ? 'step-current' : i < current ? 'step-done' : ''} aria-current={i === current ? 'step' : undefined}>
    {i < current ? <button className="progress-step-link" onClick={() => onNavigate(step.page)}><span className="step-number">✓</span><b>{step.label}</b></button> : <span className="progress-step-content"><span className="step-number">0{i + 1}</span><b>{step.label}</b></span>}
  </li>)}</ol></nav>
}
function ErrorPanel({ error, onRetry, onNavigate, onDetails }: { error: Error; onRetry: () => void; onNavigate?: (page: JourneyPage) => void; onDetails?: () => void }) {
  const view = describeError(error)
  // Without navigation (e.g. Stop catalogue on Home) retrying is the only recovery available.
  const action = onNavigate ? view.action : 'retry'
  return <div className="empty-state error-state" role="alert"><span>!</span><h2>{view.title}</h2><p>{view.message}</p>
    <div className="error-actions">
      {action === 'retry' && <button className="outline-button" onClick={onRetry}>Thử lại</button>}
      {action === 'edit-criteria' && <button className="outline-button" onClick={() => onNavigate?.('home')}>Sửa tiêu chí tìm kiếm</button>}
      {action === 'back-to-search' && <button className="outline-button" onClick={() => onNavigate?.('home')}>Về trang tìm chuyến</button>}
      {action === 'choose-other' && <>{onDetails && <button className="outline-button" onClick={onDetails}>Về chi tiết chuyến</button>}<button className="outline-button" onClick={() => onNavigate?.('results')}>Chọn chuyến khác</button></>}
    </div></div>
}
function Loading({ text }: { text: string }) { return <div className="empty-state loading-state" role="status"><h2>{text}</h2><p>Đang chờ dữ liệu từ dịch vụ.</p></div> }

export function HomePage({ criteria, setCriteria, catalogue, onSubmit, onRetry }: {
  criteria: SearchCriteria; setCriteria: (criteria: SearchCriteria) => void; catalogue: LoadState<StopOption[]>; onSubmit: () => void; onRetry: () => void
}) {
  const [message, setMessage] = useState('')
  const minDate = vietnamDate()
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!catalogue.data?.some(s => s.id === criteria.fromStopId) || !catalogue.data.some(s => s.id === criteria.toStopId)) { setMessage('Hãy chọn điểm đón và điểm trả trong danh mục.'); return }
    if (criteria.fromStopId === criteria.toStopId) { setMessage('Điểm đón và điểm trả phải khác nhau.'); return }
    if (!/^\d{4}-\d\d-\d\d$/.test(criteria.boardingDate) || criteria.boardingDate < minDate) { setMessage('Ngày lên xe không thể nằm trong quá khứ theo giờ Việt Nam.'); return }
    setMessage(''); onSubmit()
  }
  const stops = catalogue.data ?? []
  return <><section className="hero hero-centered"><div className="hero-content"><div className="eyebrow"><span className="eyebrow-dot"/>TÌM CHUYẾN DỄ DÀNG · SẴN SÀNG VI VU</div>
    <h1>Mỗi chuyến đi,<br/><em>một niềm vui</em></h1><p>Chọn điểm đón, điểm trả và ngày lên xe để tìm hành trình phù hợp với bạn.</p>
    <form className="search-card" onSubmit={submit}>
      <div className="field"><label htmlFor="from-stop">Điểm đón</label><select id="from-stop" value={criteria.fromStopId} onChange={e => setCriteria({ ...criteria, fromStopId: e.target.value })} disabled={catalogue.loading}><option value="">Chọn điểm đón</option>{stops.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}</select></div>
      <div className="field"><label htmlFor="to-stop">Điểm trả</label><select id="to-stop" value={criteria.toStopId} onChange={e => setCriteria({ ...criteria, toStopId: e.target.value })} disabled={catalogue.loading}><option value="">Chọn điểm trả</option>{stops.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}</select></div>
      <div className="field date-field"><label htmlFor="boarding-date">Ngày lên xe</label><input id="boarding-date" type="date" min={minDate} value={criteria.boardingDate} onChange={e => setCriteria({ ...criteria, boardingDate: e.target.value })}/></div>
      <button className="primary-button search-button" disabled={catalogue.loading || !stops.length}><JourneyIcon name="search"/> Tìm chuyến</button>
      <p className="prototype-note">Ngày lên xe theo giờ Việt Nam. Giá và tình trạng chuyến được xác minh khi tra cứu.</p>
      {message && <p className="form-notice" role="alert">{message}</p>}
    </form>{catalogue.loading && <p role="status">Đang tải điểm đón, điểm trả…</p>}{catalogue.error && <ErrorPanel error={catalogue.error} onRetry={onRetry}/>}
    {!catalogue.loading && catalogue.data?.length === 0 && <p role="status">Chưa có điểm phục vụ.</p>}
  </div><div className="hero-visual" aria-hidden="true"><div className="sun"/><div className="mountain mountain-back"/><div className="mountain mountain-front"/><div className="road"/><div className="bus-illustration"><div className="bus-window"/><div className="bus-window second"/><div className="bus-wheel wheel-one"/><div className="bus-wheel wheel-two"/></div></div></section>
  <section className="trust-row"><div><span className="trust-icon"><JourneyIcon name="pin"/></span><span><b>Đúng điểm đón, điểm trả</b><small>Danh mục điểm phục vụ hiện hành</small></span></div><div><span className="trust-icon"><JourneyIcon name="clock"/></span><span><b>Giá theo chặng</b><small>Giá do hệ thống xác minh</small></span></div><div><span className="trust-icon"><JourneyIcon name="seat"/></span><span><b>Tình trạng ghế chỉ xem</b><small>Không đặt vé hoặc giữ chỗ</small></span></div></section>
  <section className="popular"><div className="section-heading"><h2>Khám phá hành trình</h2></div><div className="destination-grid">{stops.slice(0, 2).map((s, i) => <article className="destination-card" key={s.id}><div className={`destination-art ${i ? 'nhatrang' : ''}`} aria-hidden="true"><span className="art-sun"/><span className="art-hill hill-a"/><span className="art-hill hill-b"/></div><div className="destination-copy"><div><b>{s.name}</b><small>Điểm phục vụ trong danh mục</small></div></div></article>)}</div></section></>
}

export function ResultsPage({ state, criteria, onNavigate, onSelectTrip, onPage, onRetry }: Navigation & {
  state: LoadState<TripSearchPage>; criteria: SearchCriteria; onSelectTrip: (trip: TripSearchItem) => void; onPage: (page: number) => void; onRetry: () => void
}) {
  const data = state.data
  return <section className="page-section"><JourneyProgress current={0} onNavigate={onNavigate}/><button className="back-link" onClick={() => onNavigate('home')}>← Quay lại</button>
    <div className="results-heading"><div><span className="section-kicker">KẾT QUẢ TÌM CHUYẾN</span><h1>Chọn chuyến cho hành trình</h1><p>Ngày lên xe: {criteria.boardingDate} · Giờ Việt Nam</p></div></div>
    {state.loading ? <Loading text="Đang tìm chuyến"/> : state.error ? <ErrorPanel error={state.error} onRetry={onRetry} onNavigate={onNavigate}/> : data && <>
      <p role="status">{data.totalElements} chuyến phù hợp</p>{data.items.length === 0 && <div className="empty-state"><h2>Không có chuyến phù hợp</h2><p>Hãy thử ngày hoặc điểm lên, xuống khác.</p></div>}
      {data.items.map(trip => <article className="trip-card" key={trip.tripId}><div className="trip-top"><span className="operator-logo"><JourneyIcon name="bus"/></span><div><b>{trip.route.name}</b><small>{trip.vehicleType}</small></div><span className="seat-count">Thông tin chuyến hiện hành</span></div>
        <div className="trip-timing"><div><b>{time(trip.boardingTime)}</b><small>{trip.fromStop.name}</small><small>{fullTime(trip.boardingTime)}</small></div><div className="duration"><span>Giờ lên / xuống</span><div/></div><div className="timing-end"><b>{time(trip.alightingTime)}</b><small>{trip.toStop.name}</small><small>{fullTime(trip.alightingTime)}</small></div><div className="trip-price"><b>{money(trip.fareAmountVnd)}</b><small>đoạn đã chọn</small></div></div>
        <div className="trip-bottom"><span>Đóng bán: {fullTime(trip.bookingClosesAt)}</span><button className="primary-button" onClick={() => onSelectTrip(trip)}>Xem chi tiết <span>→</span></button></div></article>)}
      {data.totalPages > 0 && <nav className="search-pagination" aria-label="Phân trang"><button className="outline-button" disabled={data.page === 0} onClick={() => onPage(data.page - 1)}>Trang trước</button><span>Trang {data.page + 1}/{data.totalPages}</span><button className="outline-button" disabled={data.page + 1 >= data.totalPages} onClick={() => onPage(data.page + 1)}>Trang sau</button></nav>}
    </>}
  </section>
}

export function DetailsPage({ state, onNavigate, onRetry, onOpenSeats }: Navigation & { state: LoadState<TripDetails>; onRetry: () => void; onOpenSeats: () => void }) {
  const data = state.data
  const segment = data?.selectedSegment
  const name = (id: string) => data?.stops.find(s => s.stop.id === id)?.stop.name ?? id
  const canContinue = data?.bookingEligibility.allowed && segment && segment.fareAmountVnd !== null
  return <section className="page-section"><JourneyProgress current={1} onNavigate={onNavigate}/><button className="back-link" onClick={() => onNavigate('results')}>← Quay lại</button>
    {state.loading ? <Loading text="Đang tải chi tiết chuyến"/> : state.error ? <ErrorPanel error={state.error} onRetry={onRetry} onNavigate={onNavigate}/> : data && <>
      <div className="detail-heading"><div><span className="section-kicker">CHI TIẾT CHUYẾN</span><h1>{data.route?.name ?? 'Thông tin tuyến chưa có'}</h1><p>{segment ? `${name(segment.fromStopId)} → ${name(segment.toStopId)}` : 'Chưa xác minh được đoạn đã chọn'}</p></div><span className={canContinue ? 'status-pill' : 'status-pill status-blocked'}>{canContinue ? 'Đang mở bán' : 'Không thể tiếp tục'}</span></div>
      <div className="detail-grid"><div className="detail-main"><article className="panel route-panel"><div className="panel-heading"><h2>Các điểm dừng theo thứ tự</h2><span>{data.status}</span></div>{data.stops.map(s => <div className="route-stop" key={s.stop.id}><span className="stop-marker start-marker"/><div><small>ĐIỂM DỪNG {s.index + 1}</small><b>{s.stop.name}</b><p>Đến: {s.plannedArrivalTime ? fullTime(s.plannedArrivalTime) : 'Chưa có giờ'}<br/>Rời: {s.plannedDepartureTime ? fullTime(s.plannedDepartureTime) : 'Chưa có giờ'}</p></div></div>)}{!data.stops.length && <p>Chưa có dữ liệu điểm dừng.</p>}</article>
        <article className="panel vehicle-panel"><JourneyIcon name="bus"/><div><h2>{data.vehicle?.vehicleType ?? 'Chưa có thông tin xe'}</h2><p>{data.vehicle?.seatLayoutId ? `Sơ đồ: ${data.vehicle.seatLayoutId}` : 'Chưa có sơ đồ ghế'}</p></div></article>
        <article className="panel info-panel"><h2>Thông tin chuyến xe</h2><p>{data.description ?? 'Mô tả tùy chọn chưa được cung cấp.'}</p></article>
        {data.legFares.length > 0 && <article className="panel info-panel"><h2>Giá các chặng liên tiếp</h2>{data.legFares.map(f => <p key={`${f.fromStopId}/${f.toStopId}`}>{name(f.fromStopId)} → {name(f.toStopId)}: {money(f.fareAmountVnd)}</p>)}</article>}
      </div><aside className="panel fare-panel"><span className="section-kicker">GIÁ CHẶNG ĐÃ CHỌN</span><h2>{money(segment?.fareAmountVnd ?? null)}</h2><div className="fare-divider"/>
        <div className="fare-line"><span>Giờ lên / xuống</span><b>{time(segment?.boardingTime ?? null)} – {time(segment?.alightingTime ?? null)}</b></div><div className="fare-line"><span>Đóng bán</span><b>{data.bookingClosesAt ? fullTime(data.bookingClosesAt) : 'Chưa xác định'}</b></div>
        {!segment && <p>Hãy quay lại tìm kiếm để chọn đoạn hợp lệ.</p>}{!data.bookingEligibility.allowed && <p className="eligibility-message">{data.bookingEligibility.reasons.map(r => reasonLabels[r] ?? r).join(' ')}</p>}
        <button className="primary-button full-button" disabled={!canContinue} onClick={onOpenSeats}>Xem tình trạng ghế <span>→</span></button><small className="fare-footnote">Xem ghế không giữ chỗ.</small></aside></div></>}
  </section>
}

export function SeatsPage({ selection, onNavigate }: Navigation & { selection: SeatSelection }) {
  const state = useSeatAvailability(selection)
  const data = state.data
  const statuses = new Map(data?.seats.map(s => [s.physicalSeatId, s]))
  const decks = [...new Set(data?.layout.seats.map(s => s.deck) ?? [])].sort((a, b) => a - b)
  // A rejection the user cannot retry away (409 not bookable/incomplete, 404, 400/422): show the reason
  // and the way out instead of a seat map. Transient failures keep the map with every seat UNKNOWN.
  const blocked = state.error !== null && describeError(state.error).action !== 'retry'
  const soldOut = !!data && !state.stale && !state.error && data.seats.length > 0 && data.seats.every(s => s.status === 'UNAVAILABLE')
  return <section className="page-section"><JourneyProgress current={2} onNavigate={onNavigate}/><button className="back-link" onClick={() => onNavigate('details')}>← Quay lại</button>
    <div className="detail-heading"><div><span className="section-kicker">CHỈ XEM · KHÔNG GIỮ CHỖ</span><h1>Tình trạng ghế theo chặng</h1><p>Trạng thái được xác minh lại định kỳ và khi hết hạn giữ chỗ.</p></div></div>
    {state.error && <ErrorPanel error={state.error} onRetry={state.refresh} onNavigate={onNavigate} onDetails={() => onNavigate('details')}/>}{state.loading && !data && <Loading text="Đang tải tình trạng ghế"/>}
    {state.stale && !blocked && <p className="state-message" role="status">Đang xác minh lại. Không có ghế nào được xem là trống từ dữ liệu cũ.</p>}
    {soldOut && <div className="empty-state sold-out-state" role="status"><h2>Đoạn này đã hết ghế trống</h2><p>Tất cả ghế trên đoạn đã chọn đều không khả dụng. Hãy chọn chuyến khác hoặc đổi điểm đón, điểm trả.</p>
      <div className="error-actions"><button className="outline-button" onClick={() => onNavigate('results')}>Chọn chuyến khác</button><button className="outline-button" onClick={() => onNavigate('home')}>Đổi điểm đón, điểm trả</button></div></div>}
    {data && !blocked && <div className="seat-layout"><div className="panel"><div className="seat-legend"><span><i className="legend-free"/>Còn trống</span><span><i className="legend-taken"/>Không khả dụng</span><span><i className="legend-unknown"/>Chưa xác minh</span></div>
      {decks.map(deck => { const seats = data.layout.seats.filter(s => s.deck === deck); return <div className="bus-map" key={deck}><h2>Tầng {deck + 1}</h2><div className="seat-grid" style={{ gridTemplateColumns: `repeat(${Math.max(...seats.map(s => s.column)) + 1}, minmax(0, 1fr))` }}>{seats.map(seat => {
        const status = state.stale ? 'UNKNOWN' : statuses.get(seat.physicalSeatId)?.status ?? 'UNKNOWN'
        const label = status === 'FREE' ? 'Còn trống' : status === 'UNAVAILABLE' ? 'Không khả dụng' : 'Chưa xác minh'
        const expiry = !state.stale ? statuses.get(seat.physicalSeatId)?.expiresAt : null
        return <div className="seat-cell" key={seat.physicalSeatId} style={{ gridRow: seat.row + 1, gridColumn: seat.column + 1 }}><div role="img" aria-label={`Ghế ${seat.label}: ${label}`} className={`seat seat-${status.toLowerCase()}`}><JourneyIcon name="seat"/><b>{seat.label}</b><small>{label}</small></div>{expiry && <small className="seat-expiry">Giữ đến {fullTime(expiry)}</small>}</div>
      })}</div></div>})}
      <div className="seat-bottom"><span>Cập nhật: {fullTime(data.serverTime)}</span><button className="outline-button" disabled={state.loading} onClick={state.refresh}>{state.loading ? 'Đang tải lại…' : 'Làm mới tình trạng ghế'}</button></div>
    </div><aside className="panel fare-panel"><h2>Thông tin tra cứu</h2><p>{state.stale ? 'Chưa xác minh' : `${data.seats.filter(s => s.status === 'FREE').length} ghế còn trống`}</p><p>Đóng bán: {fullTime(data.bookingClosesAt)}</p><p className="read-only-note">Xem ghế không tạo Booking hoặc giữ chỗ. Ghế có thể thay đổi ở lần kiểm tra tiếp theo.</p></aside></div>}
  </section>
}
