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
const reasonLabels: Record<string, string> = {
  TRIP_NOT_SCHEDULED: 'Chuyến không còn mở bán.',
  BOOKING_CLOSED: 'Đã qua giờ đóng bán chung của chuyến.',
  ESSENTIAL_DATA_INCOMPLETE: 'Thông tin thiết yếu chưa đủ hoặc không còn hoạt động; không thể tiếp tục.',
}

type Navigation = { onNavigate: (page: JourneyPage) => void }

function JourneyProgress({ current, onNavigate }: Navigation & { current: number }) {
  const steps = [
    { label: 'Tìm chuyến', page: 'results' as const },
    { label: 'Chi tiết chuyến', page: 'details' as const },
    { label: 'Tình trạng ghế', page: 'seats' as const },
  ]
  return (
    <nav className="journey-progress" aria-label="Tiến trình tra cứu">
      <ol>
        {steps.map((step, i) => (
          <li
            key={step.page}
            className={i === current ? 'step-current' : i < current ? 'step-done' : 'step-upcoming'}
            aria-current={i === current ? 'step' : undefined}
          >
            {i < current ? (
              <button type="button" className="progress-step-link" onClick={() => onNavigate(step.page)}>
                <span className="step-number tabular-value" aria-hidden="true">
                  <JourneyIcon name="check" />
                </span>
                <span className="step-text">
                  <small>Bước 0{i + 1}</small>
                  <b>{step.label}</b>
                </span>
              </button>
            ) : (
              <span className="progress-step-content">
                <span className="step-number tabular-value" aria-hidden="true">0{i + 1}</span>
                <span className="step-text">
                  <small>Bước 0{i + 1}</small>
                  <b>{step.label}</b>
                </span>
              </span>
            )}
          </li>
        ))}
      </ol>
    </nav>
  )
}

function ErrorPanel({ error, onRetry, onNavigate, onDetails }: {
  error: Error
  onRetry: () => void
  onNavigate?: (page: JourneyPage) => void
  onDetails?: () => void
}) {
  const view = describeError(error)
  // Without navigation (e.g. Stop catalogue on Home) retrying is the only recovery available.
  const action = onNavigate ? view.action : 'retry'
  return (
    <div className="empty-state error-state" role="alert">
      <span className="state-icon" aria-hidden="true"><JourneyIcon name="alert" /></span>
      <h2>{view.title}</h2>
      <p>{view.message}</p>
      <div className="error-actions">
        {action === 'retry' && (
          <button type="button" className="outline-button" onClick={onRetry}>
            <JourneyIcon name="refresh" />
            Thử lại
          </button>
        )}
        {action === 'edit-criteria' && (
          <button type="button" className="outline-button" onClick={() => onNavigate?.('home')}>
            Sửa tiêu chí tìm kiếm
          </button>
        )}
        {action === 'back-to-search' && (
          <button type="button" className="outline-button" onClick={() => onNavigate?.('home')}>
            Về trang tìm chuyến
          </button>
        )}
        {action === 'choose-other' && (
          <>
            {onDetails && (
              <button type="button" className="outline-button" onClick={onDetails}>
                Về chi tiết chuyến
              </button>
            )}
            <button type="button" className="outline-button" onClick={() => onNavigate?.('results')}>
              Chọn chuyến khác
            </button>
          </>
        )}
      </div>
    </div>
  )
}

function Loading({ text }: { text: string }) {
  return (
    <div className="empty-state loading-state" role="status">
      <span className="loading-indicator" aria-hidden="true"><span /><span /><span /></span>
      <h2>{text}</h2>
      <p>Đang chờ dữ liệu từ dịch vụ.</p>
    </div>
  )
}

export function HomePage({ criteria, setCriteria, catalogue, onSubmit, onRetry }: {
  criteria: SearchCriteria
  setCriteria: (criteria: SearchCriteria) => void
  catalogue: LoadState<StopOption[]>
  onSubmit: () => void
  onRetry: () => void
}) {
  const [message, setMessage] = useState('')
  const minDate = vietnamDate()

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!catalogue.data?.some(s => s.id === criteria.fromStopId) || !catalogue.data.some(s => s.id === criteria.toStopId)) {
      setMessage('Hãy chọn điểm đón và điểm trả trong danh mục.')
      return
    }
    if (criteria.fromStopId === criteria.toStopId) {
      setMessage('Điểm đón và điểm trả phải khác nhau.')
      return
    }
    if (!/^\d{4}-\d\d-\d\d$/.test(criteria.boardingDate) || criteria.boardingDate < minDate) {
      setMessage('Ngày lên xe không thể nằm trong quá khứ theo giờ Việt Nam.')
      return
    }
    setMessage('')
    onSubmit()
  }

  function resetCriteria() {
    setCriteria({ fromStopId: '', toStopId: '', boardingDate: minDate })
    setMessage('')
  }

  function swapStops() {
    setCriteria({
      ...criteria,
      fromStopId: criteria.toStopId,
      toStopId: criteria.fromStopId,
    })
    setMessage('')
  }

  const stops = catalogue.data ?? []
  const isDefaultCriteria = !criteria.fromStopId && !criteria.toStopId && criteria.boardingDate === minDate
  const canSwap = Boolean(criteria.fromStopId || criteria.toStopId) && !catalogue.loading

  return (
    <>
      <section className="hero">
        <div className="hero-inner wrap">
          <div className="hero-surface">
            <div className="hero-top">
              <div className="hero-copy">
                <p className="hero-kicker">
                  <span className="kicker-dot" aria-hidden="true" />
                  Tìm chuyến dễ dàng · Sẵn sàng vi vu
                </p>
                <h1>
                  Tra cứu chuyến xe<br />
                  <em>và giá chặng</em>
                </h1>
                <p className="hero-lead">
                  Chọn điểm đón, điểm trả và ngày lên xe để tìm hành trình phù hợp với bạn.
                </p>
              </div>
            </div>
          </div>

          <form className="search-card" onSubmit={submit}>
            <div className="search-fields">
              <div className="field">
                <label htmlFor="from-stop">
                  <JourneyIcon name="bus" className="field-label-icon" />
                  Điểm đón
                </label>
                <div className="select-wrap">
                  <select
                    id="from-stop"
                    value={criteria.fromStopId}
                    onChange={e => setCriteria({ ...criteria, fromStopId: e.target.value })}
                    disabled={catalogue.loading}
                  >
                    <option value="">Chọn điểm đón</option>
                    {stops.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
                  </select>
                </div>
              </div>

              <div className="field-swap-wrap">
                <button
                  type="button"
                  className="swap-button"
                  aria-label="Đổi điểm đón và điểm trả"
                  title="Đổi điểm đón và điểm trả"
                  disabled={!canSwap}
                  onClick={swapStops}
                >
                  <JourneyIcon name="swap" />
                </button>
              </div>

              <div className="field">
                <label htmlFor="to-stop">
                  <JourneyIcon name="pin" className="field-label-icon field-label-icon-accent" />
                  Điểm trả
                </label>
                <div className="select-wrap">
                  <select
                    id="to-stop"
                    value={criteria.toStopId}
                    onChange={e => setCriteria({ ...criteria, toStopId: e.target.value })}
                    disabled={catalogue.loading}
                  >
                    <option value="">Chọn điểm trả</option>
                    {stops.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
                  </select>
                </div>
              </div>

              <div className="field date-field">
                <label htmlFor="boarding-date">
                  <JourneyIcon name="calendar" className="field-label-icon" />
                  Ngày lên xe
                </label>
                <input
                  id="boarding-date"
                  type="date"
                  min={minDate}
                  value={criteria.boardingDate}
                  onChange={e => setCriteria({ ...criteria, boardingDate: e.target.value })}
                />
              </div>
            </div>

            <div className="search-actions-row">
              <p className="prototype-note">
                <JourneyIcon name="info" />
                <span>Ngày lên xe theo giờ Việt Nam. Giá và tình trạng chuyến được xác minh khi tra cứu.</span>
              </p>
              <div className="search-button-group">
                <button
                  type="button"
                  className="outline-button reset-button"
                  disabled={catalogue.loading || (isDefaultCriteria && !message)}
                  onClick={resetCriteria}
                >
                  <JourneyIcon name="reset" />
                  Đặt lại
                </button>
                <button
                  type="submit"
                  className="primary-button search-button"
                  disabled={catalogue.loading || !stops.length}
                >
                  <JourneyIcon name="search" />
                  Tìm chuyến
                </button>
              </div>
            </div>

            {message && <p className="form-notice" role="alert">{message}</p>}
          </form>

          {catalogue.loading && <p className="hero-status" role="status">Đang tải điểm đón, điểm trả…</p>}
          {catalogue.error && <div className="hero-error"><ErrorPanel error={catalogue.error} onRetry={onRetry} /></div>}
          {!catalogue.loading && catalogue.data?.length === 0 && <p className="hero-status" role="status">Chưa có điểm phục vụ.</p>}
        </div>
      </section>

      <section className="trust-band" aria-label="Thông tin tra cứu">
        <div className="trust-row wrap">
          <article className="trust-item">
            <span className="trust-icon trust-icon-sky" aria-hidden="true"><JourneyIcon name="pin" /></span>
            <div className="trust-copy">
              <b>Đúng điểm đón, điểm trả</b>
              <p>Danh mục điểm phục vụ hiện hành.</p>
            </div>
          </article>
          <article className="trust-item">
            <span className="trust-icon trust-icon-peach" aria-hidden="true"><JourneyIcon name="clock" /></span>
            <div className="trust-copy">
              <b>Giá theo chặng</b>
              <p>Giá do hệ thống xác minh.</p>
            </div>
          </article>
          <article className="trust-item">
            <span className="trust-icon trust-icon-mist" aria-hidden="true"><JourneyIcon name="seat" /></span>
            <div className="trust-copy">
              <b>Tình trạng ghế chỉ xem</b>
              <p>Không đặt vé hoặc giữ chỗ.</p>
            </div>
          </article>
        </div>
      </section>

      {stops.length > 0 && (
        <section className="popular-band">
          <div className="popular wrap">
            <div className="section-heading">
              <div>
                <p className="section-kicker">Danh mục điểm phục vụ</p>
                <h2>Khám phá hành trình</h2>
              </div>
              <span className="section-count tabular-value">{stops.length} điểm phục vụ</span>
            </div>
            <div className="destination-grid">
              {stops.slice(0, 3).map((s, i) => (
                <article className="destination-card" key={s.id}>
                  <div className="destination-head">
                    <span className="destination-index tabular-value">0{i + 1}</span>
                    <span className="destination-pin" aria-hidden="true"><JourneyIcon name="pin" /></span>
                  </div>
                  <div className="destination-copy">
                    <b className="clamp-2">{s.name}</b>
                    <small>Điểm phục vụ trong danh mục</small>
                  </div>
                </article>
              ))}
            </div>
          </div>
        </section>
      )}
    </>
  )
}

export function ResultsPage({ state, criteria, onNavigate, onSelectTrip, onPage, onRetry }: Navigation & {
  state: LoadState<TripSearchPage>
  criteria: SearchCriteria
  onSelectTrip: (trip: TripSearchItem) => void
  onPage: (page: number) => void
  onRetry: () => void
}) {
  const data = state.data
  return (
    <section className="page-section wrap page-enter">
      <JourneyProgress current={0} onNavigate={onNavigate} />
      <button type="button" className="back-link" onClick={() => onNavigate('home')}>
        <JourneyIcon name="arrowLeft" className="btn-arrow-left" />
        Quay lại
      </button>

      <div className="results-heading">
        <div>
          <p className="section-kicker">KẾT QUẢ TÌM CHUYẾN</p>
          <h1>Chọn chuyến cho hành trình</h1>
          <p className="page-subtitle">Ngày lên xe: <strong className="tabular-value">{criteria.boardingDate}</strong> · Giờ Việt Nam</p>
        </div>
        {data && !state.loading && !state.error && (
          <div className="results-meta" role="status">
            <span className="results-count-pill tabular-value">
              <span className="status-dot" aria-hidden="true" />
              {data.totalElements} chuyến phù hợp
            </span>
          </div>
        )}
      </div>

      {state.loading ? (
        <Loading text="Đang tìm chuyến" />
      ) : state.error ? (
        <ErrorPanel error={state.error} onRetry={onRetry} onNavigate={onNavigate} />
      ) : data && (
        <>
          {data.items.length === 0 && (
            <div className="empty-state">
              <span className="state-icon" aria-hidden="true"><JourneyIcon name="search" /></span>
              <h2>Không có chuyến phù hợp</h2>
              <p>Hãy thử ngày hoặc điểm lên, xuống khác.</p>
              <button type="button" className="outline-button" onClick={() => onNavigate('home')}>
                Đổi tiêu chí tìm kiếm
              </button>
            </div>
          )}

          <div className="trip-list">
            {data.items.map(trip => (
              <article className="trip-card" key={trip.tripId}>
                <div className="trip-top">
                  <span className="operator-logo" aria-hidden="true"><JourneyIcon name="bus" /></span>
                  <div className="trip-route-meta">
                    <b className="clamp-2">{trip.route.name}</b>
                    <small>{trip.vehicleType}</small>
                  </div>
                  <span className="seat-count">Thông tin chuyến hiện hành</span>
                </div>

                <div className="trip-timing">
                  <div className="timing-point">
                    <b className="tabular-value">{time(trip.boardingTime)}</b>
                    <span className="stop-name clamp-2">{trip.fromStop.name}</span>
                    <small className="tabular-value">{fullTime(trip.boardingTime)}</small>
                  </div>

                  <div className="duration" aria-hidden="true">
                    <span>Giờ lên / xuống</span>
                    <div className="duration-rail" />
                  </div>

                  <div className="timing-point timing-end">
                    <b className="tabular-value">{time(trip.alightingTime)}</b>
                    <span className="stop-name clamp-2">{trip.toStop.name}</span>
                    <small className="tabular-value">{fullTime(trip.alightingTime)}</small>
                  </div>

                  <div className="trip-price">
                    <b className="tabular-value">{money(trip.fareAmountVnd)}</b>
                    <small>đoạn đã chọn</small>
                  </div>
                </div>

                <div className="trip-bottom">
                  <span className="cutoff-meta">
                    <JourneyIcon name="clock" />
                    <span>Đóng bán: <strong className="tabular-value">{fullTime(trip.bookingClosesAt)}</strong></span>
                  </span>
                  <button type="button" className="primary-button" onClick={() => onSelectTrip(trip)}>
                    Xem chi tiết
                    <JourneyIcon name="arrowRight" className="btn-arrow" />
                  </button>
                </div>
              </article>
            ))}
          </div>

          {data.totalPages > 0 && (
            <nav className="search-pagination" aria-label="Phân trang">
              <button type="button" className="outline-button" disabled={data.page === 0} onClick={() => onPage(data.page - 1)}>
                Trang trước
              </button>
              <span className="pagination-status tabular-value">Trang {data.page + 1}/{data.totalPages}</span>
              <button type="button" className="outline-button" disabled={data.page + 1 >= data.totalPages} onClick={() => onPage(data.page + 1)}>
                Trang sau
              </button>
            </nav>
          )}
        </>
      )}
    </section>
  )
}

export function DetailsPage({ state, onNavigate, onRetry, onOpenSeats }: Navigation & {
  state: LoadState<TripDetails>
  onRetry: () => void
  onOpenSeats: () => void
}) {
  const data = state.data
  const segment = data?.selectedSegment
  const name = (id: string) => data?.stops.find(s => s.stop.id === id)?.stop.name ?? id
  const canContinue = Boolean(data?.bookingEligibility.allowed && segment && segment.fareAmountVnd !== null)

  return (
    <section className="page-section wrap page-enter">
      <JourneyProgress current={1} onNavigate={onNavigate} />
      <button type="button" className="back-link" onClick={() => onNavigate('results')}>
        <JourneyIcon name="arrowLeft" className="btn-arrow-left" />
        Quay lại
      </button>

      {state.loading ? (
        <Loading text="Đang tải chi tiết chuyến" />
      ) : state.error ? (
        <ErrorPanel error={state.error} onRetry={onRetry} onNavigate={onNavigate} />
      ) : data && (
        <>
          <div className="detail-heading">
            <div>
              <p className="section-kicker">CHI TIẾT CHUYẾN</p>
              <h1>{data.route?.name ?? 'Thông tin tuyến chưa có'}</h1>
              <p className="page-subtitle">
                {segment ? `${name(segment.fromStopId)} → ${name(segment.toStopId)}` : 'Chưa xác minh được đoạn đã chọn'}
              </p>
            </div>
            <span className={canContinue ? 'status-pill' : 'status-pill status-blocked'}>
              <span className="status-dot" aria-hidden="true" />
              {canContinue ? 'Đang mở bán' : 'Không thể tiếp tục'}
            </span>
          </div>

          <div className="detail-grid">
            <div className="detail-main">
              <article className="panel route-panel">
                <div className="panel-heading">
                  <h2>Các điểm dừng theo thứ tự</h2>
                  <span className="panel-tag">{data.status}</span>
                </div>
                <div className="route-timeline">
                  {data.stops.map((s, idx) => {
                    const isBoarding = segment?.fromStopId === s.stop.id
                    const isAlighting = segment?.toStopId === s.stop.id
                    const isLast = idx === data.stops.length - 1
                    const markerClass = isBoarding
                      ? 'stop-marker marker-boarding'
                      : isAlighting
                        ? 'stop-marker marker-alighting'
                        : isLast
                          ? 'stop-marker end-marker'
                          : 'stop-marker start-marker'
                    return (
                      <div
                        className={`route-stop ${isBoarding ? 'route-stop-boarding' : isAlighting ? 'route-stop-alighting' : ''}`}
                        key={s.stop.id}
                      >
                        <span className={markerClass} aria-hidden="true" />
                        <div className="route-stop-body">
                          <div className="route-stop-top">
                            <small className="tabular-value">ĐIỂM DỪNG {s.index + 1}</small>
                            {isBoarding && <span className="stop-role-tag tag-boarding">Điểm đón</span>}
                            {isAlighting && <span className="stop-role-tag tag-alighting">Điểm trả</span>}
                          </div>
                          <b>{s.stop.name}</b>
                          <div className="route-stop-times">
                            <span>Đến: <strong className="tabular-value">{s.plannedArrivalTime ? fullTime(s.plannedArrivalTime) : 'Chưa có giờ'}</strong></span>
                            <span>Rời: <strong className="tabular-value">{s.plannedDepartureTime ? fullTime(s.plannedDepartureTime) : 'Chưa có giờ'}</strong></span>
                          </div>
                        </div>
                      </div>
                    )
                  })}
                  {!data.stops.length && <p className="muted-empty">Chưa có dữ liệu điểm dừng.</p>}
                </div>
              </article>

              <article className="panel vehicle-panel">
                <span className="vehicle-icon" aria-hidden="true"><JourneyIcon name="bus" /></span>
                <div className="vehicle-copy">
                  <h2>{data.vehicle?.vehicleType ?? 'Chưa có thông tin xe'}</h2>
                  <p>
                    {data.vehicle?.seatLayoutId ? (
                      <>Sơ đồ: <code className="inline-code">{data.vehicle.seatLayoutId}</code></>
                    ) : 'Chưa có sơ đồ ghế'}
                  </p>
                </div>
              </article>

              <article className="panel info-panel">
                <h2>Thông tin chuyến xe</h2>
                <p>{data.description ?? 'Mô tả tùy chọn chưa được cung cấp.'}</p>
              </article>

              {data.legFares.length > 0 && (
                <article className="panel info-panel">
                  <h2>Giá các chặng liên tiếp</h2>
                  <ul className="leg-fare-list">
                    {data.legFares.map(f => (
                      <li key={`${f.fromStopId}/${f.toStopId}`}>
                        <span>{name(f.fromStopId)} → {name(f.toStopId)}</span>
                        <b className="tabular-value">{money(f.fareAmountVnd)}</b>
                      </li>
                    ))}
                  </ul>
                </article>
              )}
            </div>

            <aside className={`panel fare-panel ${canContinue ? 'summary-ready' : 'summary-blocked'}`}>
              <p className="section-kicker">GIÁ CHẶNG ĐÃ CHỌN</p>
              <h2 className="tabular-value">{money(segment?.fareAmountVnd ?? null)}</h2>
              <div className="fare-divider" />
              <div className="fare-line">
                <span>Giờ lên / xuống</span>
                <b className="tabular-value">{time(segment?.boardingTime ?? null)} – {time(segment?.alightingTime ?? null)}</b>
              </div>
              <div className="fare-line">
                <span>Đóng bán</span>
                <b className="tabular-value">{data.bookingClosesAt ? fullTime(data.bookingClosesAt) : 'Chưa xác định'}</b>
              </div>
              {!segment && <p className="eligibility-message">Hãy quay lại tìm kiếm để chọn đoạn hợp lệ.</p>}
              {!data.bookingEligibility.allowed && (
                <p className="eligibility-message">
                  {data.bookingEligibility.reasons.map(r => reasonLabels[r] ?? r).join(' ')}
                </p>
              )}
              <button type="button" className="primary-button full-button" disabled={!canContinue} onClick={onOpenSeats}>
                Xem tình trạng ghế
                <JourneyIcon name="arrowRight" className="btn-arrow" />
              </button>
              <small className="fare-footnote">Xem ghế không giữ chỗ.</small>
            </aside>
          </div>
        </>
      )}
    </section>
  )
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
  const freeCount = data ? data.seats.filter(s => s.status === 'FREE').length : 0

  return (
    <section className="page-section wrap page-enter">
      <JourneyProgress current={2} onNavigate={onNavigate} />
      <button type="button" className="back-link" onClick={() => onNavigate('details')}>
        <JourneyIcon name="arrowLeft" className="btn-arrow-left" />
        Quay lại
      </button>

      <div className="detail-heading">
        <div>
          <p className="section-kicker">CHỈ XEM · KHÔNG GIỮ CHỖ</p>
          <h1>Tình trạng ghế theo chặng</h1>
          <p className="page-subtitle">Trạng thái được xác minh lại định kỳ và khi hết hạn giữ chỗ.</p>
        </div>
      </div>

      {state.error && (
        <ErrorPanel
          error={state.error}
          onRetry={state.refresh}
          onNavigate={onNavigate}
          onDetails={() => onNavigate('details')}
        />
      )}
      {state.loading && !data && <Loading text="Đang tải tình trạng ghế" />}
      {state.stale && data && !blocked && (
        <p className="state-message" role="status">
          <JourneyIcon name="info" />
          <span>Đang xác minh lại. Không có ghế nào được xem là trống từ dữ liệu cũ.</span>
        </p>
      )}
      {soldOut && (
        <div className="empty-state sold-out-state" role="status">
          <span className="state-icon" aria-hidden="true"><JourneyIcon name="seat" /></span>
          <h2>Đoạn này đã hết ghế trống</h2>
          <p>Tất cả ghế trên đoạn đã chọn đều không khả dụng. Hãy chọn chuyến khác hoặc đổi điểm đón, điểm trả.</p>
          <div className="error-actions">
            <button type="button" className="outline-button" onClick={() => onNavigate('results')}>Chọn chuyến khác</button>
            <button type="button" className="outline-button" onClick={() => onNavigate('home')}>Đổi điểm đón, điểm trả</button>
          </div>
        </div>
      )}

      {data && !blocked && (
        <div className="seat-layout">
          <div className="panel seat-panel">
            <div className="seat-legend" aria-label="Chú giải tình trạng ghế">
              <span className="legend-item"><i className="legend-free" aria-hidden="true" />Còn trống</span>
              <span className="legend-item"><i className="legend-taken" aria-hidden="true" />Không khả dụng</span>
              <span className="legend-item"><i className="legend-unknown" aria-hidden="true" />Chưa xác minh</span>
            </div>

            <div className="deck-stack">
              {decks.map(deck => {
                const seats = data.layout.seats.filter(s => s.deck === deck)
                const colCount = Math.max(...seats.map(s => s.column)) + 1
                return (
                  <div className="bus-map" key={deck}>
                    <div className="driver-label">
                      <h2>Tầng {deck + 1}</h2>
                      <span className="driver-front" aria-hidden="true">
                        <JourneyIcon name="steering" />
                        <small>Đầu xe</small>
                      </span>
                    </div>
                    <div
                      className="seat-grid"
                      style={{ gridTemplateColumns: `repeat(${colCount}, minmax(0, 1fr))` }}
                    >
                      {seats.map(seat => {
                        const status = state.stale ? 'UNKNOWN' : statuses.get(seat.physicalSeatId)?.status ?? 'UNKNOWN'
                        const label = status === 'FREE' ? 'Còn trống' : status === 'UNAVAILABLE' ? 'Không khả dụng' : 'Chưa xác minh'
                        const expiry = !state.stale ? statuses.get(seat.physicalSeatId)?.expiresAt : null
                        return (
                          <div
                            className="seat-cell"
                            key={seat.physicalSeatId}
                            style={{ gridRow: seat.row + 1, gridColumn: seat.column + 1 }}
                          >
                            <div
                              role="img"
                              aria-label={`Ghế ${seat.label}: ${label}`}
                              className={`seat seat-${status.toLowerCase()}`}
                            >
                              <JourneyIcon name="seat" />
                              <b>{seat.label}</b>
                              <small>{label}</small>
                            </div>
                            {expiry && <small className="seat-expiry tabular-value">Giữ đến {fullTime(expiry)}</small>}
                          </div>
                        )
                      })}
                    </div>
                    <div className="rear-label" aria-hidden="true">Cuối xe</div>
                  </div>
                )
              })}
            </div>

            <div className="seat-bottom">
              <span className="seat-updated">
                <JourneyIcon name="clock" />
                <span>Cập nhật: <strong className="tabular-value">{fullTime(data.serverTime)}</strong></span>
              </span>
              <button type="button" className="outline-button" disabled={state.loading} onClick={state.refresh}>
                <JourneyIcon name="refresh" />
                {state.loading ? 'Đang tải lại…' : 'Làm mới tình trạng ghế'}
              </button>
            </div>
          </div>

          <aside className="panel seat-summary">
            <p className="section-kicker">Tóm tắt tình trạng</p>
            <h2>Thông tin tra cứu</h2>
            <div className={`seat-count-highlight tabular-value ${state.stale ? 'highlight-unknown' : freeCount > 0 ? 'highlight-free' : 'highlight-taken'}`}>
              <b>{state.stale ? 'Chưa xác minh' : `${freeCount} ghế còn trống`}</b>
            </div>
            <div className="fare-divider" />
            <div className="fare-line">
              <span>Đóng bán</span>
              <b className="tabular-value">{fullTime(data.bookingClosesAt)}</b>
            </div>
            <p className="read-only-note">
              Xem ghế không tạo Booking hoặc giữ chỗ. Ghế có thể thay đổi ở lần kiểm tra tiếp theo.
            </p>
          </aside>
        </div>
      )}
    </section>
  )
}
