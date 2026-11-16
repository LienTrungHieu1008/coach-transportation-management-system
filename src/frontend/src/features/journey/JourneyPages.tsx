import { useMemo, useState, type FormEvent } from 'react'
import type { SearchCriteria, SeatAvailability, TripDetails, TripSearchItem } from '../../shared/api/journeyTypes'
import { JourneyIcon } from './JourneyIcon'
import { demoAvailability, demoBoardingDate, demoDetails, demoStops, demoTrip } from './demoData'

export type JourneyPage = 'home' | 'schedule' | 'results' | 'details' | 'seats'

type CommonProps = {
  criteria: SearchCriteria
  setCriteria: (criteria: SearchCriteria) => void
  onNavigate: (page: JourneyPage) => void
}

const LOCAL_TIME_ZONE = 'Asia/Ho_Chi_Minh'
const money = (amount: number | null) => amount === null ? 'Chưa có giá' : `${new Intl.NumberFormat('vi-VN').format(amount)}đ`
const dateInVietnam = (date: Date) => new Intl.DateTimeFormat('en-CA', { timeZone: LOCAL_TIME_ZONE, year: 'numeric', month: '2-digit', day: '2-digit' }).format(date)
const displayTime = (instant: string | null) => instant === null ? 'Chưa có giờ' : new Intl.DateTimeFormat('vi-VN', { timeZone: LOCAL_TIME_ZONE, hour: '2-digit', minute: '2-digit', hourCycle: 'h23' }).format(new Date(instant))
const displayDate = (date: string) => new Intl.DateTimeFormat('vi-VN', { timeZone: LOCAL_TIME_ZONE, weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }).format(new Date(`${date}T12:00:00+07:00`))

function JourneyProgress({ current, onNavigate }: { current: 0 | 1 | 2; onNavigate: (page: JourneyPage) => void }) {
  const steps = [
    { code: 'P04', label: 'Tìm chuyến', page: 'results' as const },
    { code: 'P05', label: 'Chi tiết chuyến', page: 'details' as const },
    { code: 'P07', label: 'Tình trạng ghế', page: 'seats' as const },
  ]
  return <nav className="journey-progress" aria-label="Tiến trình tra cứu"><ol>{steps.map((step, index) => {
    const contents = <><span className="step-number">{index < current ? '✓' : `0${index + 1}`}</span><span><small>UC-{step.code}</small><b>{step.label}</b></span></>
    return <li className={index === current ? 'step-current' : index < current ? 'step-done' : ''} aria-current={index === current ? 'step' : undefined} key={step.code}>
      {index < current ? <button className="progress-step-link" type="button" onClick={() => onNavigate(step.page)} aria-label={`Quay lại bước ${step.label}`}>{contents}</button> : <span className="progress-step-content">{contents}</span>}
    </li>
  })}</ol></nav>
}

function DemoLabel() {
  return <span className="prototype-note"><strong>Prototype · dữ liệu ví dụ OpenAPI 0.1</strong> · Chưa lấy từ API hoặc DB đang chạy.</span>
}

function SearchForm({ criteria, setCriteria, onSubmit }: { criteria: SearchCriteria; setCriteria: (value: SearchCriteria) => void; onSubmit: () => void }) {
  const [message, setMessage] = useState('')
  const minDate = dateInVietnam(new Date())
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const from = demoStops.find((stop) => stop.id === criteria.fromStopId)
    const to = demoStops.find((stop) => stop.id === criteria.toStopId)
    if (!from || !to) { setMessage('Hãy chọn điểm đón và điểm trả trong danh sách điểm phục vụ.'); return }
    if (from.id === to.id) { setMessage('Điểm đón và điểm trả phải khác nhau.'); return }
    if (from.id !== demoTrip.fromStop.id || to.id !== demoTrip.toStop.id) { setMessage('Thứ tự điểm này không thuộc tuyến trong dữ liệu prototype. Chọn Demo Stop A rồi Demo Stop B.'); return }
    if (!criteria.boardingDate || criteria.boardingDate < minDate) { setMessage('Ngày lên xe không thể nằm trong quá khứ theo giờ Việt Nam.'); return }
    setMessage('')
    onSubmit()
  }
  return <form className="search-card" onSubmit={submit}>
    <div className="field"><label htmlFor="from-stop">Điểm đón</label><select id="from-stop" value={criteria.fromStopId} onChange={(event) => setCriteria({ ...criteria, fromStopId: event.target.value })}><option value="">Chọn điểm đón</option>{demoStops.map((stop) => <option key={stop.id} value={stop.id}>{stop.name}</option>)}</select></div>
    <div className="field"><label htmlFor="to-stop">Điểm trả</label><select id="to-stop" value={criteria.toStopId} onChange={(event) => setCriteria({ ...criteria, toStopId: event.target.value })}><option value="">Chọn điểm trả</option>{demoStops.map((stop) => <option key={stop.id} value={stop.id}>{stop.name}</option>)}</select></div>
    <div className="field date-field"><label htmlFor="boarding-date">Ngày lên xe</label><input id="boarding-date" type="date" min={minDate} value={criteria.boardingDate} onChange={(event) => setCriteria({ ...criteria, boardingDate: event.target.value })}/></div>
    <button className="primary-button search-button" type="submit"><JourneyIcon name="search"/> Tìm chuyến</button>
    {message && <p className="form-notice" role="alert">{message}</p>}
  </form>
}

export function HomePage(props: CommonProps) {
  return <>
    <section className="hero hero-centered">
      <div className="hero-content">
        <div className="eyebrow"><span className="eyebrow-dot"/> TÌM CHUYẾN DỄ DÀNG · SẴN SÀNG VI VU</div>
        <h1>Mỗi chuyến đi,<br/><em>một niềm vui</em></h1>
        <p>Chọn điểm đón, điểm trả và ngày khởi hành để tìm hành trình phù hợp với bạn.</p>
        <SearchForm criteria={props.criteria} setCriteria={props.setCriteria} onSubmit={() => props.onNavigate('results')}/>
      </div>
      <div className="hero-visual" aria-hidden="true"><div className="sun"/><div className="mountain mountain-back"/><div className="mountain mountain-front"/><div className="road"/><div className="bus-illustration"><div className="bus-window"/><div className="bus-window second"/><div className="bus-wheel wheel-one"/><div className="bus-wheel wheel-two"/></div></div>
    </section>
    <section className="trust-row"><div><span className="trust-icon"><JourneyIcon name="pin"/></span><span><b>Đúng điểm đón, điểm trả</b><small>Chọn từ danh mục điểm phục vụ</small></span></div><div><span className="trust-icon"><JourneyIcon name="clock"/></span><span><b>Giá theo chặng</b><small>Hiển thị fare của đoạn đã chọn</small></span></div><div><span className="trust-icon"><JourneyIcon name="seat"/></span><span><b>Tình trạng ghế chỉ xem</b><small>Không tạo booking hoặc giữ chỗ</small></span></div></section>
    <section className="popular"><div className="section-heading"><div><h2>Khám phá hành trình</h2></div></div><div className="destination-grid"><article className="destination-card"><div className="destination-art"><span className="art-sun"/><span className="art-hill hill-a"/><span className="art-hill hill-b"/><span className="art-pine">A</span></div><div className="destination-copy"><div><b>Demo Stop A</b><small>Điểm lên xe trong dữ liệu prototype</small></div></div></article><article className="destination-card"><div className="destination-art nhatrang"><span className="art-sun"/><span className="sea-line"/><span className="sea-wave">B</span></div><div className="destination-copy"><div><b>Demo Stop B</b><small>Điểm xuống xe trong dữ liệu prototype</small></div></div></article></div><DemoLabel/></section>
  </>
}

type ScheduleTrip = {
  id: string
  route: string
  from: string
  to: string
  date: string
  departure: string
  arrival: string
  vehicle: string
  fare: number
}

const scheduleTrips: ScheduleTrip[] = [
  { id: 'sample-1', route: 'Sài Gòn – Nha Trang', from: 'Bến xe Miền Đông mới', to: 'Bến xe phía Nam Nha Trang', date: '2026-11-20', departure: '21:00', arrival: '05:30', vehicle: 'Xe giường nằm', fare: 320000 },
  { id: 'sample-2', route: 'Sài Gòn – Đà Lạt', from: 'Bến xe Miền Đông mới', to: 'Bến xe liên tỉnh Đà Lạt', date: '2026-11-21', departure: '22:00', arrival: '05:00', vehicle: 'Limousine giường nằm', fare: 290000 },
  { id: 'sample-3', route: 'Sài Gòn – Cần Thơ', from: 'Bến xe Miền Tây', to: 'Bến xe trung tâm Cần Thơ', date: '2026-11-21', departure: '07:00', arrival: '11:30', vehicle: 'Limousine giường phòng', fare: 190000 },
  { id: 'sample-4', route: 'Nha Trang – Đà Lạt', from: 'Bến xe phía Nam Nha Trang', to: 'Bến xe liên tỉnh Đà Lạt', date: '2026-11-22', departure: '08:00', arrival: '12:00', vehicle: 'Limousine', fare: 180000 },
]

export function SchedulePage({ onNavigate }: Pick<CommonProps, 'onNavigate'>) {
  const formatFare = (amount: number) => `${new Intl.NumberFormat('vi-VN').format(amount)}đ`
  return <section className="page-section schedule-page">
    <div className="schedule-heading">
      <div><span className="section-kicker">LỊCH XE BUS TRIP</span><h1>Lịch trình chuyến xe</h1><p>Tham khảo các chuyến xe đang có trong lịch mẫu.</p></div>
      <button className="primary-button schedule-search" type="button" onClick={() => onNavigate('home')}><JourneyIcon name="search"/> Tìm chuyến</button>
    </div>
    <div className="schedule-list-heading"><b>{scheduleTrips.length} chuyến mẫu</b><span>Dữ liệu minh họa · Chưa kết nối cơ sở dữ liệu</span></div>
    <div className="schedule-list">{scheduleTrips.map((trip) => <article className="schedule-card" key={trip.id}>
      <div className="schedule-card-top"><div><span className="schedule-route-label">{trip.route}</span><span className="schedule-vehicle">{trip.vehicle}</span></div><span className="schedule-status">Theo lịch mẫu</span></div>
      <div className="schedule-stops"><div><small>ĐIỂM ĐÓN</small><b>{trip.from}</b></div><span className="schedule-route-line" aria-hidden="true"><i/></span><div><small>ĐIỂM TRẢ</small><b>{trip.to}</b></div></div>
      <div className="schedule-card-bottom"><span className="schedule-date"><JourneyIcon name="clock"/>{displayDate(trip.date)}</span><span className="schedule-time"><b>{trip.departure}</b><span>→</span><b>{trip.arrival}</b></span><strong className="schedule-fare">{formatFare(trip.fare)} <small>/ khách</small></strong></div>
    </article>)}</div>
    <p className="schedule-note">Lịch trình và giá vé trên trang này chỉ là dữ liệu giả lập để minh họa giao diện.</p>
  </section>
}

export function ResultsPage({ criteria, onNavigate, onSelectTrip }: CommonProps & { onSelectTrip: (trip: TripSearchItem) => void }) {
  const [retrievalFailed, setRetrievalFailed] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const from = demoStops.find((stop) => stop.id === criteria.fromStopId)
  const to = demoStops.find((stop) => stop.id === criteria.toStopId)
  const matches = criteria.boardingDate === demoBoardingDate && from?.id === demoTrip.fromStop.id && to?.id === demoTrip.toStop.id
  return <section className="page-section"><JourneyProgress current={0} onNavigate={onNavigate}/><button className="back-link" onClick={() => onNavigate('home')}>← Quay lại</button><div className="results-heading"><div><span className="section-kicker">UC-P04 · KẾT QUẢ TÌM CHUYẾN</span><h1>{from?.name ?? '—'} <span>→</span> {to?.name ?? '—'}</h1><p>{criteria.boardingDate ? displayDate(criteria.boardingDate) : 'Chưa chọn ngày'} · {matches ? '1 chuyến trong ví dụ contract' : 'Không có chuyến trong dữ liệu ví dụ'}</p></div><div className="result-date"><JourneyIcon name="clock"/> Ngày lên xe <b>{criteria.boardingDate || '—'}</b></div></div>
    {isLoading ? <div className="empty-state loading-state" role="status"><span>◌</span><h3>Đang tìm chuyến</h3><p>Đang chờ kết quả từ dịch vụ Trip.</p></div> : retrievalFailed ? <div className="empty-state error-state" role="alert"><span>!</span><h3>Không thể xác minh chuyến xe</h3><p>Không hiển thị kết quả mẫu như thể là dữ liệu hiện tại. Hãy thử lại hoặc sửa tiêu chí.</p><button className="outline-button" onClick={() => { setRetrievalFailed(false); setIsLoading(false) }}>Thử lại ví dụ</button> <button className="outline-button" onClick={() => onNavigate('home')}>Sửa tiêu chí</button></div> : matches ? <article className="trip-card"><div className="trip-top"><span className="operator-logo">EX</span><div><b>{demoTrip.route.name}</b><small>{demoTrip.vehicleType} <span className="rating">Scheduled</span></small></div><span className="seat-count">Giờ/giá từ snapshot</span></div><div className="trip-timing"><div><b>{displayTime(demoTrip.boardingTime)}</b><small>{demoTrip.fromStop.name}</small></div><div className="duration"><span>Giờ khởi hành / đến</span><div/><small>Theo dữ liệu Trip</small></div><div className="timing-end"><b>{displayTime(demoTrip.alightingTime)}</b><small>{demoTrip.toStop.name}</small></div><div className="trip-price"><b>{money(demoTrip.fareAmountVnd)}</b><small>toàn chặng đã chọn</small></div></div><div className="trip-bottom"><span>Đóng bán: {displayTime(demoTrip.bookingClosesAt)} · {demoTrip.bookingClosesAt.slice(0, 10)} (giờ VN)</span><button className="primary-button" onClick={() => { onSelectTrip(demoTrip); onNavigate('details') }}>Xem chi tiết <span>→</span></button></div></article> : <div className="empty-state"><span>⌕</span><h3>Không có chuyến phù hợp trong ví dụ này</h3><p>Hãy sửa ngày hoặc điểm dừng. Không tìm thấy dữ liệu mẫu cho tiêu chí đã chọn.</p><button className="outline-button" onClick={() => onNavigate('home')}>Sửa tiêu chí</button></div>}
    <DemoLabel/><details className="demo-controls"><summary>Preview nhánh UC-P04 (chỉ dùng review prototype)</summary><div className="demo-control-buttons"><button className="outline-button" onClick={() => { setIsLoading(false); setRetrievalFailed(false) }}>Dữ liệu trả về</button><button className="outline-button" onClick={() => { setIsLoading(false); setRetrievalFailed(true) }}>Lỗi không truy xuất được (503)</button><button className="outline-button" onClick={() => { setIsLoading(true); setRetrievalFailed(false) }}>Đang chờ response</button></div></details></section>
}

export function DetailsPage({ criteria, onNavigate }: CommonProps) {
  const [detailState, setDetailState] = useState<TripDetails>(demoDetails)
  const [detailError, setDetailError] = useState<'missing' | 'retrieval' | null>(null)
  const segment = detailState.selectedSegment
  const from = demoStops.find((stop) => stop.id === criteria.fromStopId)
  const to = demoStops.find((stop) => stop.id === criteria.toStopId)
  const canContinue = detailState.bookingEligibility.allowed && segment !== null && segment.fareAmountVnd !== null
  const reasonLabels: Record<string, string> = { TRIP_NOT_SCHEDULED: 'Chuyến không còn ở trạng thái Scheduled.', BOOKING_CLOSED: 'Đã qua giờ đóng bán chung của chuyến.', ESSENTIAL_DATA_INCOMPLETE: 'Thiếu thông tin thiết yếu; không thể tiếp tục xem ghế.' }
  const selectedStops = useMemo(() => detailState.stops.filter((stop) => stop.index >= 0), [detailState.stops])
  return <section className="page-section"><JourneyProgress current={1} onNavigate={onNavigate}/><button className="back-link" onClick={() => onNavigate('results')}>← Quay lại</button>{detailError ? <div className="empty-state error-state" role="alert"><span>!</span><h3>{detailError === 'missing' ? 'Không tìm thấy chuyến này' : 'Không thể tải chi tiết chuyến'}</h3><p>{detailError === 'missing' ? 'Chuyến có thể đã không còn trong hệ thống. Hãy quay lại tìm kiếm.' : 'Không thể xác minh dữ liệu Trip, giờ hoặc giá; không hiển thị dữ liệu mẫu.'}</p><button className="outline-button" onClick={() => onNavigate('results')}>Về kết quả tìm kiếm</button> <button className="outline-button" onClick={() => setDetailError(null)}>Thử lại ví dụ</button></div> : <><div className="detail-heading"><div><span className="section-kicker">UC-P05 · CHI TIẾT CHUYẾN</span><h1>{from?.name ?? '—'} <span>→</span> {to?.name ?? '—'}</h1><p>{detailState.route?.name ?? 'Thông tin tuyến chưa có'} · {detailState.vehicle?.vehicleType ?? 'Chưa có xe được xác nhận'}</p></div><span className={canContinue ? 'status-pill' : 'status-pill status-blocked'}>{canContinue ? '● Đang mở bán' : '● Không thể tiếp tục'}</span></div><div className="detail-grid"><div className="detail-main"><article className="panel route-panel"><div className="panel-heading"><h2>Các điểm dừng theo thứ tự</h2><span>{detailState.status}</span></div>{selectedStops.length ? selectedStops.map((stop, index) => <div className="route-stop" key={stop.stop.id}><span className={`stop-marker ${index === selectedStops.length - 1 ? 'end-marker' : 'start-marker'}`}/><div><small>{index === 0 ? 'ĐIỂM LÊN' : index === selectedStops.length - 1 ? 'ĐIỂM XUỐNG' : 'ĐIỂM DỪNG'} · {displayTime(stop.plannedDepartureTime ?? stop.plannedArrivalTime)}</small><b>{stop.stop.name}</b><p>{stop.plannedArrivalTime === null ? 'Giờ đến chưa được cung cấp' : `Đến ${displayTime(stop.plannedArrivalTime)}${stop.plannedDepartureTime ? ` · Rời ${displayTime(stop.plannedDepartureTime)}` : ''}`}</p></div></div>) : <p className="state-message">Chưa có dữ liệu điểm dừng để hiển thị.</p>}</article><article className="panel vehicle-panel"><span className="vehicle-icon">▰</span><div><h2>{detailState.vehicle?.vehicleType ?? 'Chưa có thông tin xe'}</h2><p>{detailState.vehicle?.seatLayoutId ? `Mã sơ đồ ghế: ${detailState.vehicle.seatLayoutId}` : 'Chưa xác định sơ đồ ghế'}</p></div></article><article className="panel info-panel"><h2>Thông tin chuyến xe</h2><p>{detailState.description ?? 'Mô tả tùy chọn chưa được cung cấp.'}</p></article>{detailState.legFares.length > 0 && <article className="panel info-panel"><h2>Giá các chặng liên tiếp</h2>{detailState.legFares.map((fare) => <p key={`${fare.fromStopId}-${fare.toStopId}`}>{demoStops.find((stop) => stop.id === fare.fromStopId)?.name ?? fare.fromStopId} → {demoStops.find((stop) => stop.id === fare.toStopId)?.name ?? fare.toStopId}: {money(fare.fareAmountVnd)}</p>)}</article>}</div><aside className="panel fare-panel"><span className="section-kicker">GIÁ CHẶNG ĐÃ CHỌN</span><h2>{money(segment?.fareAmountVnd ?? null)}</h2><div className="fare-divider"/><div className="fare-line"><span>Điểm lên / xuống</span><b>{from?.name ?? '—'} → {to?.name ?? '—'}</b></div><div className="fare-line"><span>Trạng thái chuyến</span><b>{detailState.status}</b></div><div className="fare-line"><span>Đóng bán</span><b>{detailState.bookingClosesAt ? `${displayTime(detailState.bookingClosesAt)} · ${detailState.bookingClosesAt.slice(0, 10)}` : 'Chưa xác định'}</b></div>{!detailState.bookingEligibility.allowed && <p className="eligibility-message">{detailState.bookingEligibility.reasons.map((reason) => reasonLabels[reason] ?? 'Không đủ điều kiện tiếp tục.').join(' ')}</p>}<button className="primary-button full-button" disabled={!canContinue} onClick={() => onNavigate('seats')}>Xem tình trạng ghế <span>→</span></button><small className="fare-footnote">Xem availability không giữ chỗ</small></aside></div></>}<DemoLabel/><details className="demo-controls"><summary>Preview nhánh UC-P05 (chỉ dùng review prototype)</summary><div className="demo-control-buttons"><button className="outline-button" onClick={() => { setDetailError(null); setDetailState(demoDetails) }}>Dữ liệu đầy đủ / đang mở</button><button className="outline-button" onClick={() => { setDetailError(null); setDetailState({ ...demoDetails, status: 'Cancelled', bookingEligibility: { allowed: false, reasons: ['TRIP_NOT_SCHEDULED'] } }) }}>Trip đã đóng</button><button className="outline-button" onClick={() => { setDetailError(null); setDetailState({ ...demoDetails, stops: [], legFares: [], vehicle: null, selectedSegment: null, bookingEligibility: { allowed: false, reasons: ['ESSENTIAL_DATA_INCOMPLETE'] } }) }}>Thiếu dữ liệu thiết yếu</button><button className="outline-button" onClick={() => { setDetailError(null); setDetailState({ ...demoDetails, description: null }) }}>Thiếu mô tả tùy chọn</button><button className="outline-button" onClick={() => setDetailError('missing')}>Trip không còn tồn tại</button><button className="outline-button" onClick={() => setDetailError('retrieval')}>Lỗi tải chi tiết (503)</button></div></details></section>
}

export function SeatsPage({ criteria, onNavigate, trip }: CommonProps & { trip: TripSearchItem }) {
  const [availability, setAvailability] = useState<SeatAvailability | null>(demoAvailability)
  const [retrievalFailed, setRetrievalFailed] = useState(false)
  const [updatedAt, setUpdatedAt] = useState(demoAvailability.serverTime)
  const from = demoStops.find((stop) => stop.id === criteria.fromStopId)
  const to = demoStops.find((stop) => stop.id === criteria.toStopId)
  const statusBySeat = new Map(availability?.seats.map((seat) => [seat.physicalSeatId, seat]) ?? [])
  const freeCount = availability?.seats.filter((seat) => seat.status === 'FREE').length ?? 0
  const seatStateLabel = { FREE: 'Còn trống', UNAVAILABLE: 'Không khả dụng', UNKNOWN: 'Chưa xác minh' }
  function refresh() {
    // The refresh action replays the same static contract snapshot; it does not pretend to query the backend.
    setAvailability(demoAvailability)
    setRetrievalFailed(false)
    setUpdatedAt(demoAvailability.serverTime)
  }
  return <section className="page-section"><JourneyProgress current={2} onNavigate={onNavigate}/><button className="back-link" onClick={() => onNavigate('details')}>← Quay lại</button><div className="detail-heading"><div><span className="section-kicker">UC-P07 · TRẠNG THÁI GHẾ</span><h1>Tình trạng ghế theo chặng</h1><p>{from?.name ?? '—'} → {to?.name ?? '—'} · {displayTime(trip.boardingTime)}, {criteria.boardingDate}</p></div><span className={availability ? 'status-pill' : 'status-pill status-blocked'}>{availability ? `Snapshot · ${freeCount} ghế trống` : 'Availability chưa xác minh'}</span></div><div className="seat-layout"><div className="seat-main panel"><div className="panel-heading"><h2>Sơ đồ xe · {availability?.layout.seats[0]?.deck === 0 ? 'Tầng 1' : 'Tầng chưa xác định'}</h2><span>Chỉ xem</span></div>{retrievalFailed || availability === null ? <div className="availability-error" role="status"><span className="unknown-seat">?</span><h3>Chưa xác minh được tình trạng ghế</h3><p>Không thể tải dữ liệu sơ đồ hoặc phân bổ. Không có ghế nào được xem là trống.</p><button className="outline-button" onClick={refresh}>Thử tải lại</button></div> : <><div className="seat-legend"><span><i className="legend-free"/> Còn trống</span><span><i className="legend-taken"/> Không khả dụng</span><span><i className="legend-unknown"/> Chưa xác minh</span></div><div className="bus-map"><div className="driver-label">ĐẦU XE <span>◉</span></div><div className="seat-grid contract-seat-grid">{availability.layout.seats.map((seat) => { const state = statusBySeat.get(seat.physicalSeatId)?.status ?? 'UNKNOWN'; const expiresAt = statusBySeat.get(seat.physicalSeatId)?.expiresAt; return <div key={seat.physicalSeatId} className={`seat read-only-seat seat-${state.toLowerCase()}`} aria-label={`Ghế ${seat.label}: ${seatStateLabel[state]}`} title={`${seat.label}: ${seatStateLabel[state]}${expiresAt ? ` · Hạn giữ đến ${displayTime(expiresAt)}` : ''}`} style={{ gridColumn: seat.column + 1, gridRow: seat.row + 1 }}>{seat.label}</div> })}</div><div className="rear-label">CUỐI XE</div></div><div className="seat-bottom"><span>Trạng thái cập nhật: {displayTime(updatedAt)} · giờ server trong snapshot</span><button className="outline-button" onClick={refresh}>Làm mới snapshot ↻</button></div></> }<p className="prototype-note">Tình trạng chỉ phản ánh Segment đang xem tại một thời điểm; tải lại trong prototype phát lại cùng dữ liệu mẫu.</p></div><aside className="panel seat-summary"><span className="section-kicker">CHẶNG ĐANG XEM</span><h2>{from?.name ?? '—'} <span>→</span> {to?.name ?? '—'}</h2><p>{displayTime(trip.boardingTime)} – {displayTime(trip.alightingTime)} · {trip.vehicleType}</p><div className="fare-divider"/><div className="fare-line"><span>Ghế còn trống</span><b>{availability ? freeCount : '—'}</b></div><div className="fare-line"><span>Ghế không khả dụng</span><b>{availability?.seats.filter((seat) => seat.status === 'UNAVAILABLE').length ?? '—'}</b></div><div className="fare-line"><span>Ghế chưa xác minh</span><b>{availability?.seats.filter((seat) => seat.status === 'UNKNOWN').length ?? '—'}</b></div><p className="read-only-note">Xem ghế không tạo Booking hoặc giữ chỗ. Tình trạng cần được kiểm tra lại khi đặt.</p></aside></div><DemoLabel/><details className="demo-controls"><summary>Preview nhánh UC-P07 (chỉ dùng review prototype)</summary><div className="demo-control-buttons"><button className="outline-button" onClick={() => { setAvailability(demoAvailability); setRetrievalFailed(false) }}>Có ghế trống / đã dùng</button><button className="outline-button" onClick={() => { setAvailability({ ...demoAvailability, seats: demoAvailability.seats.map((seat) => ({ ...seat, status: 'UNAVAILABLE' })) }); setRetrievalFailed(false) }}>Hết ghế</button><button className="outline-button" onClick={() => { setAvailability(null); setRetrievalFailed(true) }}>Không tải được availability</button></div></details></section>
}


