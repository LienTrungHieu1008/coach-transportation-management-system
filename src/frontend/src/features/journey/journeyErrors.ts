import { JourneyApiError } from '../../shared/api/journey'

/** How the UI should let the user recover from an error. */
export type ErrorAction = 'retry' | 'edit-criteria' | 'back-to-search' | 'choose-other'
export type ErrorView = { title: string; message: string; action: ErrorAction }

const fieldLabels: Record<string, string> = {
  fromStopId: 'điểm đón', toStopId: 'điểm trả', boardingDate: 'ngày lên xe', page: 'trang kết quả', size: 'số kết quả mỗi trang', tripId: 'mã chuyến',
}

/**
 * Maps an error to Vietnamese copy chosen by HTTP status and contract code.
 * Server `message`/`fieldErrors` text is never shown: modules return it in different
 * languages and it may repeat itself. Only field names are reused to point at inputs.
 */
export function describeError(error: Error): ErrorView {
  if (error instanceof TypeError) {
    return { title: 'Không thể tải dữ liệu', message: 'Không thể kết nối dịch vụ. Hãy thử lại.', action: 'retry' }
  }
  if (!(error instanceof JourneyApiError)) return { title: 'Không thể tải dữ liệu', message: error.message, action: 'retry' }
  const code = error.body?.code
  switch (code) {
    case 'INVALID_REQUEST': {
      const fields = [...new Set((error.body?.fieldErrors ?? []).flatMap(f => fieldLabels[f.field] ? [fieldLabels[f.field]] : []))]
      return { title: 'Thông tin tra cứu chưa hợp lệ', message: fields.length ? `Hãy kiểm tra lại ${fields.join(', ')}.` : 'Hãy kiểm tra lại thông tin tra cứu.', action: 'edit-criteria' }
    }
    case 'INVALID_SEGMENT':
      return { title: 'Đoạn đường chưa hợp lệ', message: 'Điểm đón phải đứng trước điểm trả trên cùng tuyến. Hãy chọn lại điểm đón, điểm trả.', action: 'edit-criteria' }
    case 'STOP_NOT_SERVED':
      return { title: 'Điểm dừng chưa được phục vụ', message: 'Điểm đón hoặc điểm trả không thuộc tuyến đang phục vụ. Hãy chọn điểm khác trong danh mục.', action: 'edit-criteria' }
    case 'PAST_BOARDING_DATE':
      return { title: 'Ngày lên xe đã qua', message: 'Hãy chọn hôm nay hoặc một ngày sau theo giờ Việt Nam.', action: 'edit-criteria' }
    case 'TRIP_NOT_FOUND':
      return { title: 'Chuyến không còn khả dụng', message: 'Không tìm thấy chuyến này. Hãy quay lại tìm kiếm để chọn chuyến khác.', action: 'back-to-search' }
    case 'TRIP_NOT_BOOKABLE':
      return { title: 'Chuyến không còn mở bán', message: 'Chuyến đã đóng bán hoặc không còn ở trạng thái mở bán nên không thể xem sơ đồ ghế. Hãy chọn chuyến hoặc đoạn khác.', action: 'choose-other' }
    case 'ESSENTIAL_DATA_INCOMPLETE':
      return { title: 'Chuyến thiếu dữ liệu bắt buộc', message: 'Hệ thống chưa đủ thông tin để hiển thị sơ đồ ghế của chuyến này. Hãy chọn chuyến hoặc đoạn khác.', action: 'choose-other' }
    case 'TRIP_DATA_UNAVAILABLE':
      return { title: 'Không thể tải dữ liệu', message: 'Chưa xác minh được thông tin chuyến xe. Hãy thử lại sau ít phút.', action: 'retry' }
    case 'AVAILABILITY_UNAVAILABLE':
      return { title: 'Không thể tải dữ liệu', message: 'Chưa xác minh được tình trạng ghế. Hãy thử lại sau ít phút.', action: 'retry' }
    case 'INTERNAL_ERROR':
      return { title: 'Không thể tải dữ liệu', message: 'Dịch vụ đang gặp sự cố. Hãy thử lại.', action: 'retry' }
  }
  if (error.status === 409) return { title: 'Không thể xem sơ đồ ghế', message: 'Chuyến này hiện không thể tiếp tục. Hãy chọn chuyến hoặc đoạn khác.', action: 'choose-other' }
  if (error.status === 404) return { title: 'Chuyến không còn khả dụng', message: 'Hãy quay lại tìm kiếm để chọn chuyến khác.', action: 'back-to-search' }
  if (error.status === 400 || error.status === 422) return { title: 'Thông tin tra cứu chưa hợp lệ', message: 'Hãy kiểm tra lại thông tin tra cứu.', action: 'edit-criteria' }
  // No server body (proxy/network failure) or an unrecognised code: the client message is already Vietnamese.
  return { title: 'Không thể tải dữ liệu', message: error.body ? 'Dịch vụ đang gặp sự cố. Hãy thử lại.' : error.message, action: 'retry' }
}
