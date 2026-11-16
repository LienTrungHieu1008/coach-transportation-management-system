import { DetailsPage, HomePage, ResultsPage, SchedulePage, SeatsPage } from './features/journey/JourneyPages'
import { JourneyIcon } from './features/journey/JourneyIcon'
import { useJourneyState } from './features/journey/useJourneyState'
import './features/journey/journey.css'

function App() {
  const journey = useJourneyState()
  return <div className="app-shell">
    <header className="topbar"><a className="brand" href="#home" onClick={(event) => { event.preventDefault(); journey.setPage('home') }}><span className="brand-mark"><JourneyIcon name="bus"/></span><span><b>BusTrip</b><small>Dịch vụ đặt xe uy tín</small></span></a><nav className="main-nav" aria-label="Điều hướng chính"><button className={journey.page !== 'schedule' ? 'nav-active' : ''} onClick={() => journey.setPage('home')}>Tìm chuyến</button><button className={journey.page === 'schedule' ? 'nav-active' : ''} onClick={() => journey.setPage('schedule')}>Lịch trình</button></nav><span className="role-label">Hành khách · Xem công khai</span></header>
    <main>
      {journey.page === 'home' && <HomePage criteria={journey.criteria} setCriteria={journey.setCriteria} onNavigate={journey.setPage}/>}
      {journey.page === 'schedule' && <SchedulePage onNavigate={journey.setPage}/>}
      {journey.page === 'results' && <ResultsPage criteria={journey.criteria} setCriteria={journey.setCriteria} onNavigate={journey.setPage} onSelectTrip={journey.setTrip}/>}
      {journey.page === 'details' && <DetailsPage criteria={journey.criteria} setCriteria={journey.setCriteria} onNavigate={journey.setPage}/>}
      {journey.page === 'seats' && <SeatsPage criteria={journey.criteria} setCriteria={journey.setCriteria} onNavigate={journey.setPage} trip={journey.trip}/>}
    </main>
    <footer className="footer"><a className="brand footer-brand" href="#home" onClick={(event) => { event.preventDefault(); journey.setPage('home') }}><span className="brand-mark"><JourneyIcon name="bus"/></span><b>BusTrip</b></a><span>Public journey · P04 / P05 / P07</span><span>Prototype PA4 · Dữ liệu ví dụ, chưa nối API</span></footer>
  </div>
}

export default App
