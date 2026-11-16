# F1 — cải thiện thiết kế, 17/11/2026

Người giao việc chọn thử hướng đề xuất: hiện đại, gọn, đáng tin cậy; navy–cam, form tìm nổi bật, chữ dễ đọc, thẻ chuyến và các màn sau thống nhất. Chỉ thiết kế lại prototype hiện có, không nối BE hoặc thay đổi nghiệp vụ.

## Hướng thực hiện

Đã cân nhắc giữ bố cục cũ và chỉ chỉnh token (ít khác biệt, vẫn nhiều vùng trang trí), so với tổ chức lại hierarchy bằng CSS và markup trình bày tối thiểu. Chọn cách thứ hai theo đề xuất được người dùng đồng ý: hero navy có minh họa xe tiết chế, form trắng nổi bật; các màn nghiệp vụ nền trung tính, dữ liệu giờ/giá rõ hơn. Không thêm thư viện, font hoặc tài nguyên mạng.

## Thiết kế

- Bốn màn Home, Results, Details, Seats và trang Schedule hiện có dùng cùng typography hệ thống hỗ trợ tiếng Việt; body 16px, thông tin phụ tối thiểu 12px, nút/control tối thiểu 44px.
- Navy cho thương hiệu và cấu trúc, cam đậm cho CTA, màu semantic riêng cho FREE/UNAVAILABLE/UNKNOWN và lỗi; giữ nhãn chữ cùng màu. Tương phản text mục tiêu 4.5:1, focus dễ thấy.
- Home nhấn form chọn Stop/ngày và CTA. Trang Results nhấn giờ/giá/hành trình; Details có timeline và sidebar rõ; Seats dùng layout/ID cũ, ghế chỉ xem và chú giải rõ.
- Nhãn prototype/ngày mô phỏng và các preview states giữ nguyên. Không tạo hành động booking/hold, thông số quảng cáo hoặc dữ liệu thật giả.
- Responsive tại 375/768/1024/1440px; form xếp dọc ở điện thoại, sidebar xuống hàng, không che nội dung; giữ dark mode và reduced-motion.

## Bất biến và nghiệm thu

Giữ logic/data/state và OpenAPI. 15 tests hiện có phải pass cùng lint/typecheck/build; thay CSS không cần test phản chiếu selector. Tạo lỗi/nghiệp vụ mới phải có test riêng. Kiểm tra trực quan nếu môi trường cho phép; chưa xem phải ghi rõ. Không commit/push/merge trong task thiết kế khi chưa được giao.
