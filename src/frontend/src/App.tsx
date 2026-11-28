import { useEffect, useRef, useState, type FormEvent } from 'react'
import { DetailsPage, HomePage, ResultsPage, SeatsPage } from './features/journey/JourneyPages'
import { JourneyIcon } from './features/journey/JourneyIcon'
import { useJourneyState } from './features/journey/useJourneyState'
import './features/journey/journey.css'

function App() {
  const journey = useJourneyState()
  const main = useRef<HTMLElement>(null)
  const [guideOpen, setGuideOpen] = useState(false)
  const [quickQuery, setQuickQuery] = useState('')

  useEffect(() => {
    main.current?.focus()
  }, [journey.page])

  useEffect(() => {
    if (!guideOpen) return
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setGuideOpen(false)
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [guideOpen])

  function handleQuickSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const term = quickQuery.trim().toLowerCase()
    if (term && journey.catalogue.data?.length) {
      const matched = journey.catalogue.data.find(s => s.name.toLowerCase().includes(term))
      if (matched) {
        journey.setDraftCriteria({ ...journey.draftCriteria, fromStopId: matched.id })
      }
    }
    if (journey.page !== 'home') {
      journey.navigate('home')
    }
    window.requestAnimationFrame(() => {
      const target = document.getElementById('from-stop')
      target?.focus()
      target?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    })
  }

  const selected = journey.detail.data?.selectedSegment

  return (
    <div className="app-shell">
      <a className="skip-link" href="#main-content">Đến nội dung chính</a>
      <header className="topbar">
        <div className="topbar-inner wrap">
          <a
            className="brand"
            href="#home"
            onClick={e => {
              e.preventDefault()
              journey.navigate('home')
            }}
          >
            <span className="brand-mark" aria-hidden="true">
              <JourneyIcon name="bus" />
            </span>
            <span className="brand-copy">
              <b>BusTrip</b>
              <small>Hành trình của bạn</small>
            </span>
          </a>

          <form className="header-search" role="search" aria-label="Tìm kiếm chuyến đi" onSubmit={handleQuickSearch}>
            <input
              type="search"
              className="header-search-input"
              value={quickQuery}
              onChange={e => setQuickQuery(e.target.value)}
              placeholder="Tìm kiếm chuyến đi..."
              aria-label="Tìm kiếm chuyến đi..."
            />
          </form>

          <div className="topbar-utilities">
            <button
              type="button"
              className="icon-pill-button"
              aria-label="Thông báo và hướng dẫn tra cứu"
              aria-expanded={guideOpen}
              onClick={() => setGuideOpen(true)}
            >
              <JourneyIcon name="bell" />
              <span className="notification-dot" aria-hidden="true" />
            </button>
            <span className="role-label">
              <span className="role-dot" aria-hidden="true" />
              Hành khách · Xem công khai
            </span>
            <span className="profile-badge" aria-hidden="true">
              <JourneyIcon name="user" />
            </span>
          </div>
        </div>
      </header>

      <main id="main-content" ref={main} tabIndex={-1}>
        {journey.page === 'home' && (
          <HomePage
            criteria={journey.draftCriteria}
            setCriteria={journey.setDraftCriteria}
            catalogue={journey.catalogue}
            onSubmit={journey.submitSearch}
            onRetry={() => journey.retry('catalogue')}
          />
        )}
        {journey.page === 'results' && journey.submittedCriteria && (
          <ResultsPage
            criteria={journey.submittedCriteria}
            state={journey.search}
            onNavigate={journey.navigate}
            onSelectTrip={journey.selectTrip}
            onPage={journey.changeSearchPage}
            onRetry={() => journey.retry('search')}
          />
        )}
        {journey.page === 'details' && (
          <DetailsPage
            state={journey.detail}
            onNavigate={journey.navigate}
            onRetry={() => journey.retry('detail')}
            onOpenSeats={journey.openSeats}
          />
        )}
        {journey.page === 'seats' && journey.selectedTrip && selected && (
          <SeatsPage
            selection={{
              tripId: journey.selectedTrip.tripId,
              fromStopId: selected.fromStopId,
              toStopId: selected.toStopId,
            }}
            onNavigate={journey.navigate}
          />
        )}
      </main>

      <footer className="footer">
        <div className="footer-inner wrap">
          <div className="footer-brand-group">
            <a
              className="brand footer-brand"
              href="#home"
              onClick={e => {
                e.preventDefault()
                journey.navigate('home')
              }}
            >
              <span className="brand-mark" aria-hidden="true">
                <JourneyIcon name="bus" />
              </span>
              <span className="brand-copy">
                <b>BusTrip</b>
                <small>Nền tảng tra cứu hành trình liên tỉnh</small>
              </span>
            </a>
          </div>
          <div className="footer-meta">
            <span className="footer-links">Tra cứu chuyến · Chi tiết · Tình trạng ghế</span>
            <span className="footer-scope">Chỉ tra cứu · Không đặt vé hoặc giữ chỗ</span>
          </div>
          <div className="footer-bottom">
            <span>Hỗ trợ hành khách · Giờ chuẩn Việt Nam (UTC+7)</span>
            <span>© 2026 BusTrip</span>
          </div>
        </div>
      </footer>

      {guideOpen && (
        <div
          className="modal-backdrop"
          role="presentation"
          onClick={e => {
            if (e.target === e.currentTarget) setGuideOpen(false)
          }}
        >
          <div
            className="modal-card"
            role="dialog"
            aria-modal="true"
            aria-labelledby="guide-modal-title"
          >
            <div className="modal-header">
              <div className="modal-title-group">
                <span className="modal-icon" aria-hidden="true">
                  <JourneyIcon name="info" />
                </span>
                <div>
                  <p className="section-kicker">Thông báo hệ thống</p>
                  <h2 id="guide-modal-title">Hướng dẫn tra cứu công khai</h2>
                </div>
              </div>
              <button
                type="button"
                className="modal-close"
                aria-label="Đóng hộp thoại"
                onClick={() => setGuideOpen(false)}
              >
                <JourneyIcon name="close" />
              </button>
            </div>
            <ul className="modal-list">
              <li>
                <span className="modal-bullet" aria-hidden="true"><JourneyIcon name="check" /></span>
                <span>Mọi mốc giờ khởi hành, điểm dừng và đóng bán đều hiển thị theo giờ Việt Nam (UTC+7).</span>
              </li>
              <li>
                <span className="modal-bullet" aria-hidden="true"><JourneyIcon name="check" /></span>
                <span>Giá hiển thị là giá niêm yết của chặng đã chọn từ hệ thống hiện hành.</span>
              </li>
              <li>
                <span className="modal-bullet" aria-hidden="true"><JourneyIcon name="check" /></span>
                <span>Chế độ hiện tại chỉ phục vụ tra cứu lịch chạy và xem tình trạng ghế, không tạo đặt vé hay giữ chỗ.</span>
              </li>
            </ul>
            <div className="modal-actions">
              <button type="button" className="primary-button" onClick={() => setGuideOpen(false)}>
                Đã hiểu
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default App
