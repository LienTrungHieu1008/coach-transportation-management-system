import { useEffect, useState } from 'react'
import { checkSystemHealth } from './shared/api/health'

type ConnectionState = 'loading' | 'ready' | 'unavailable'

const connection = {
  loading: { label: 'Đang kiểm tra kết nối', style: 'bg-slate-100 text-slate-700' },
  ready: { label: 'Kết nối sẵn sàng', style: 'bg-emerald-50 text-emerald-800' },
  unavailable: { label: 'Chưa kết nối được dịch vụ', style: 'bg-amber-50 text-amber-900' },
}

function App() {
  const [state, setState] = useState<ConnectionState>('loading')
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    checkSystemHealth(controller.signal)
      .then(() => { if (!controller.signal.aborted) setState('ready') })
      .catch(() => { if (!controller.signal.aborted) setState('unavailable') })
    return () => controller.abort()
  }, [attempt])

  function retry() {
    setState('loading')
    setAttempt((value) => value + 1)
  }

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-5xl items-center gap-3 px-6 py-5">
          <span aria-hidden="true" className="grid size-10 place-items-center rounded-xl bg-blue-700 font-bold text-white">11</span>
          <div>
            <p className="font-semibold tracking-tight">Coach Transportation</p>
            <p className="text-sm text-slate-500">Team 11 · Bản nền tảng</p>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-6 py-12 sm:py-20">
        <p className="mb-4 text-sm font-semibold uppercase tracking-widest text-blue-700">Smart Bus Booking & Operations</p>
        <h1 className="max-w-3xl text-4xl font-semibold leading-tight tracking-tight sm:text-5xl">Đặt vé và vận hành nhà xe</h1>
        <p className="mt-6 max-w-2xl text-lg leading-8 text-slate-600">
          Không gian chung cho hành khách, nhân viên và quản trị viên. Các chức năng đang được xây dựng theo từng use case.
        </p>
        <section aria-labelledby="connection-title" className="mt-10 rounded-2xl border border-slate-200 bg-white p-6 sm:p-8">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <h2 id="connection-title" className="text-lg font-semibold">Trạng thái dịch vụ</h2>
            <p role="status" className={`rounded-full px-4 py-2 text-sm font-medium ${connection[state].style}`}>
              {connection[state].label}
            </p>
          </div>
          <p className="mt-4 leading-7 text-slate-600">
            Đây là bộ khung kết nối giao diện với dịch vụ, chưa phải bản demo nghiệp vụ PA4.
          </p>
          {state === 'unavailable' && (
            <button type="button" onClick={retry} className="mt-5 rounded-lg bg-blue-700 px-5 py-2.5 font-medium text-white hover:bg-blue-800 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-blue-700">
              Thử lại
            </button>
          )}
        </section>
        <section aria-labelledby="next-title" className="mt-12">
          <h2 id="next-title" className="text-lg font-semibold">Các chức năng tiếp theo</h2>
          <div className="mt-5 grid gap-4 sm:grid-cols-3">
            {['Tìm chuyến', 'Xem chuyến và ghế', 'Quản lý đặt chỗ'].map((label, index) => (
              <article key={label} className="rounded-xl border border-slate-200 bg-white p-5">
                <p aria-hidden="true" className="text-sm font-semibold text-blue-700">0{index + 1}</p>
                <h3 className="mt-3 font-medium">{label}</h3>
                <p className="mt-2 text-sm text-slate-500">Chưa triển khai</p>
              </article>
            ))}
          </div>
        </section>
      </main>
      <footer className="mx-auto max-w-5xl px-6 pb-8 text-sm text-slate-500">Team 11 · Nhập môn Công nghệ phần mềm · PA4</footer>
    </div>
  )
}

export default App
