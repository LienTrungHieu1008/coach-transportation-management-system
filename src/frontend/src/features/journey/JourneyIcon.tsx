import type { ReactNode } from 'react'

type JourneyIconName = 'bus' | 'search' | 'pin' | 'clock' | 'seat' | 'spark' | 'steering'

const paths: Record<JourneyIconName, ReactNode> = {
  bus: <><path d="M5 17V6.5A2.5 2.5 0 0 1 7.5 4h9A2.5 2.5 0 0 1 19 6.5V17"/><path d="M5 10h14M7 17v2m10-2v2M7.5 14h.01M16.5 14h.01M5 17h14l1 2H4l1-2Z"/></>,
  search: <><circle cx="10.8" cy="10.8" r="6.3"/><path d="m16 16 4 4"/></>,
  pin: <><path d="M19 10c0 5-7 11-7 11S5 15 5 10a7 7 0 1 1 14 0Z"/><circle cx="12" cy="10" r="2.2"/></>,
  clock: <><circle cx="12" cy="12" r="8.5"/><path d="M12 7v5l3.5 2"/></>,
  seat: <><path d="M7 13V6.5A2.5 2.5 0 0 1 9.5 4H13a2 2 0 0 1 2 2v5M7 10l2 7h9a2 2 0 0 0 2-2v-1a2 2 0 0 0-2-2H7Zm2 7v3m9-3v3"/></>,
  spark: <><path d="m12 3 1.4 5.6L19 10l-5.6 1.4L12 17l-1.4-5.6L5 10l5.6-1.4L12 3Z"/><path d="m19 16 .7 2.3L22 19l-2.3.7L19 22l-.7-2.3L16 19l2.3-.7L19 16Z"/></>,
  steering: <><circle cx="12" cy="12" r="8.5"/><circle cx="12" cy="12" r="2.1"/><path d="M4 9.5 10 11m10-1.5L14 11m-2 3v6"/></>,
}

export function JourneyIcon({ name }: { name: JourneyIconName }) {
  return <svg aria-hidden="true" focusable="false" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">{paths[name]}</svg>
}
