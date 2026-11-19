import { useEffect, useRef } from 'react'
import { DetailsPage, HomePage, ResultsPage, SeatsPage } from './features/journey/JourneyPages'
import { JourneyIcon } from './features/journey/JourneyIcon'
import { useJourneyState } from './features/journey/useJourneyState'
import './features/journey/journey.css'

function App() {
  const journey = useJourneyState()
  const main = useRef<HTMLElement>(null)
  useEffect(() => { main.current?.focus() }, [journey.page])
  const selected = journey.detail.data?.selectedSegment
  return <div className="app-shell"><a className="skip-link" href="#main-content">Đến nội dung chính</a>
    <header className="topbar"><a className="brand" href="#home" onClick={e => { e.preventDefault(); journey.navigate('home') }}><span className="brand-mark"><JourneyIcon name="bus"/></span><span><b>BusTrip</b><small>Hành trình của bạn</small></span></a><nav className="main-nav" aria-label="Điều hướng chính"><button className="nav-active" onClick={() => journey.navigate('home')}>Tìm chuyến</button></nav><span className="role-label">Hành khách · Xem công khai</span></header>
    <main id="main-content" ref={main} tabIndex={-1}>
      {journey.page === 'home' && <HomePage criteria={journey.draftCriteria} setCriteria={journey.setDraftCriteria} catalogue={journey.catalogue} onSubmit={journey.submitSearch} onRetry={() => journey.retry('catalogue')}/>}
      {journey.page === 'results' && journey.submittedCriteria && <ResultsPage criteria={journey.submittedCriteria} state={journey.search} onNavigate={journey.navigate} onSelectTrip={journey.selectTrip} onPage={journey.changeSearchPage} onRetry={() => journey.retry('search')}/>}
      {journey.page === 'details' && <DetailsPage state={journey.detail} onNavigate={journey.navigate} onRetry={() => journey.retry('detail')} onOpenSeats={journey.openSeats}/>}
      {journey.page === 'seats' && journey.selectedTrip && selected && <SeatsPage selection={{ tripId: journey.selectedTrip.tripId, fromStopId: selected.fromStopId, toStopId: selected.toStopId }} onNavigate={journey.navigate}/>}
    </main>
    <footer className="footer"><a className="brand footer-brand" href="#home" onClick={e => { e.preventDefault(); journey.navigate('home') }}><span className="brand-mark"><JourneyIcon name="bus"/></span><b>BusTrip</b></a><span>Tra cứu chuyến · Chi tiết · Tình trạng ghế</span><span>Chỉ tra cứu · Không đặt vé hoặc giữ chỗ</span></footer>
  </div>
}
export default App
