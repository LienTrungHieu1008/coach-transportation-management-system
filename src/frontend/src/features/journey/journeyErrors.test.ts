import { describe, expect, it } from 'vitest'
import { JourneyApiError } from '../../shared/api/journey'
import { describeError } from './journeyErrors'

const api = (status: number, code?: string) =>
  new JourneyApiError('server text', status, code ? { code, message: 'server text', fieldErrors: [] } : null)

describe('describeError', () => {
  it.each([
    [422, 'INVALID_SEGMENT', 'edit-criteria'],
    [422, 'STOP_NOT_SERVED', 'edit-criteria'],
    [422, 'PAST_BOARDING_DATE', 'edit-criteria'],
    [400, 'INVALID_REQUEST', 'edit-criteria'],
    [404, 'TRIP_NOT_FOUND', 'back-to-search'],
    [409, 'TRIP_NOT_BOOKABLE', 'choose-other'],
    [409, 'ESSENTIAL_DATA_INCOMPLETE', 'choose-other'],
    [503, 'TRIP_DATA_UNAVAILABLE', 'retry'],
    [503, 'AVAILABILITY_UNAVAILABLE', 'retry'],
    [500, 'INTERNAL_ERROR', 'retry'],
  ])('%i %s -> %s, never echoing server text', (status, code, action) => {
    const view = describeError(api(status, code))
    expect(view.action).toBe(action)
    expect(view.message).not.toContain('server text')
  })
  it('falls back by status for an unknown code', () => {
    expect(describeError(api(409, 'SOMETHING_NEW')).action).toBe('choose-other')
    expect(describeError(api(422, 'SOMETHING_NEW')).action).toBe('edit-criteria')
  })
  it('keeps the client message when there is no server body or no API error', () => {
    expect(describeError(new JourneyApiError('Không thể tải dữ liệu chuyến xe.', 502, null)))
      .toEqual({ title: 'Không thể tải dữ liệu', message: 'Không thể tải dữ liệu chuyến xe.', action: 'retry' })
    expect(describeError(new Error('Dịch vụ phản hồi quá lâu. Hãy thử lại.')).message).toBe('Dịch vụ phản hồi quá lâu. Hãy thử lại.')
  })
})
