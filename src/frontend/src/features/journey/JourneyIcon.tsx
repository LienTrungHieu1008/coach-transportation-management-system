import type { ReactNode } from 'react'

export type JourneyIconName =
  | 'bus'
  | 'search'
  | 'pin'
  | 'clock'
  | 'seat'
  | 'spark'
  | 'steering'
  | 'refresh'
  | 'info'
  | 'alert'
  | 'arrowRight'
  | 'arrowLeft'
  | 'calendar'
  | 'bell'
  | 'reset'
  | 'check'
  | 'close'
  | 'swap'
  | 'user'

const paths: Record<JourneyIconName, ReactNode> = {
  bus: <><path d="M5 17V6.5A2.5 2.5 0 0 1 7.5 4h9A2.5 2.5 0 0 1 19 6.5V17"/><path d="M5 10h14M7 17v2m10-2v2M7.5 14h.01M16.5 14h.01M5 17h14l1 2H4l1-2Z"/></>,
  search: <><circle cx="10.8" cy="10.8" r="6.3"/><path d="m16 16 4 4"/></>,
  pin: <><path d="M19 10c0 5-7 11-7 11S5 15 5 10a7 7 0 1 1 14 0Z"/><circle cx="12" cy="10" r="2.2"/></>,
  clock: <><circle cx="12" cy="12" r="8.5"/><path d="M12 7v5l3.5 2"/></>,
  seat: <><path d="M7 13V6.5A2.5 2.5 0 0 1 9.5 4H13a2 2 0 0 1 2 2v5M7 10l2 7h9a2 2 0 0 0 2-2v-1a2 2 0 0 0-2-2H7Zm2 7v3m9-3v3"/></>,
  spark: <><path d="m12 3 1.4 5.6L19 10l-5.6 1.4L12 17l-1.4-5.6L5 10l5.6-1.4L12 3Z"/><path d="M19 16 .7 2.3L22 19l-2.3.7L19 22l-.7-2.3L16 19l2.3-.7L19 16Z"/></>,
  steering: <><circle cx="12" cy="12" r="8.5"/><circle cx="12" cy="12" r="2.1"/><path d="M4 9.5 10 11m10-1.5L14 11m-2 3v6"/></>,
  refresh: <><path d="M20 11a8 8 0 1 0 2 5.3"/><path d="M20 4v7h-7"/></>,
  info: <><circle cx="12" cy="12" r="9"/><path d="M12 16v-4"/><path d="M12 8h.01"/></>,
  alert: <><path d="M12 9v4"/><path d="M12 17h.01"/><path d="M10.3 3.9 2.6 17.2A2 2 0 0 0 4.3 20h15.4a2 2 0 0 0 1.7-2.8L13.7 3.9a2 2 0 0 0-3.4 0Z"/></>,
  arrowRight: <><path d="M5 12h14"/><path d="m13 6 6 6-6 6"/></>,
  arrowLeft: <><path d="M19 12H5"/><path d="m11 18-6-6 6-6"/></>,
  calendar: <><rect x="3.5" y="5" width="17" height="15.5" rx="2.5"/><path d="M8 3.5v3M16 3.5v3M3.5 9.5h17"/></>,
  bell: <><path d="M18 8a6 6 0 1 0-12 0c0 7-3 8.5-3 8.5h18s-3-1.5-3-8.5"/><path d="M13.7 20a2 2 0 0 1-3.4 0"/></>,
  reset: <><path d="M3 12a9 9 0 1 0 3-6.7L3 8"/><path d="M3 3v5h5"/></>,
  check: <><path d="M5 12.5 9.5 17 19 7.5"/></>,
  close: <><path d="M6 6l12 12M18 6 6 18"/></>,
  swap: <><path d="M7 16V4m0 0-3 3m3-3 3 3M17 8v12m0 0-3-3m3 3 3-3"/></>,
  user: <><circle cx="12" cy="8.5" r="3.5"/><path d="M5 19.5c1.6-3 4.1-4.5 7-4.5s5.4 1.5 7 4.5"/></>,
}

export function JourneyIcon({ name, className }: { name: JourneyIconName; className?: string }) {
  return (
    <svg
      className={className}
      aria-hidden="true"
      focusable="false"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      {paths[name]}
    </svg>
  )
}
