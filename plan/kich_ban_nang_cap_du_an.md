# KỊCH BẢN NÂNG CẤP DỰ ÁN E-COMMERCE MICROSERVICES

---

## Kịch Bản 1: Hệ Thống Đánh Giá, Nhận Xét Và Lượt Đã Bán Của Sản Phẩm (Review, Rating & Total Sold)

- **Mục tiêu**: Tăng độ uy tín và tin cậy của sản phẩm. Cho phép xem số lượt mua thực tế và chỉ cho phép người thực sự mua hàng được để lại đánh giá.

- **Nghiệp vụ Backend**:
  - Gộp trực tiếp module Review vào `product_service` để tận dụng lưu trữ Cloudinary và đồng bộ bộ đệm Redis.
  - Cập nhật bảng `products` bổ sung 3 trường: `avg_rating` (điểm sao trung bình), `total_reviews` (tổng lượt đánh giá), và `total_sold` (tổng số lượng sản phẩm đã bán).
  - **Quy tắc xác minh mua hàng nghiêm ngặt (Verified Purchase)**:
    - Khi khách gửi đánh giá, `product_service` gọi Feign sang `order_service` kiểm tra: đúng `userId`, đúng mã đơn `orderId`, đơn đã ở trạng thái `COMPLETED` và có chứa `productId` đó.
    - Ràng buộc duy nhất `UNIQUE(order_id, product_id, user_id)` để mỗi món trong đơn chỉ được đánh giá 1 lần.
  - **Cơ chế cập nhật lượt mua (`total_sold`) qua Kafka**:
    - Khi đơn hàng hoàn tất (`COMPLETED` hoặc `PAID`), `order_service` phát sự kiện kèm danh sách sản phẩm và số lượng mua (`quantity`).
    - `product_service` lắng nghe sự kiện Kafka để cộng dồn `total_sold` và xóa cache Redis tương ứng.
  - **Quản trị viên (Admin)**:
    - Admin có quyền xem toàn bộ đánh giá, ẩn/hiện các đánh giá vi phạm (khi ẩn sẽ loại số sao ra khỏi điểm trung bình của sản phẩm) và viết phản hồi chính thức cho khách hàng.

- **Nghiệp vụ Frontend**:
  - Thẻ sản phẩm và trang chi tiết sản phẩm hiển thị: Điểm sao trung bình, tổng số đánh giá và số lượng "Đã bán" (ví dụ: ⭐ 4.9 | 120 Đánh giá | Đã bán 350).
  - Không mở ô nhập đánh giá tự do tại trang chi tiết sản phẩm.
  - Nút "Đánh giá sản phẩm" chỉ hiển thị tại trang Lịch sử đơn hàng bên cạnh các món hàng thuộc đơn đã hoàn thành (`COMPLETED`). Sau khi đánh giá xong, nút tự chuyển sang nhãn "Đã đánh giá".

---

## Kịch Bản 2: Quản Lý Biến Thể Sản Phẩm (Kích Thước, Màu Sắc - SKU)

- **Mục tiêu**: Phục vụ đúng ngành hàng thời trang, cho phép một sản phẩm có nhiều kích cỡ và màu sắc khác nhau, mỗi loại có số lượng tồn kho riêng.

- **Nghiệp vụ Backend**:
  - Tạo bảng biến thể sản phẩm lưu thông tin: mã SKU, kích thước (Size S, M, L, XL), màu sắc, giá bán chênh lệch, hình ảnh riêng cho từng màu.
  - Chuyển logic quản lý tồn kho sang cấp độ mã SKU thay vì theo mã sản phẩm chung.
  - Cập nhật giỏ hàng và đơn hàng để lưu chính xác biến thể mà khách hàng lựa chọn.
  - Giữ chỗ và trừ kho chính xác theo từng mã SKU khi tạo đơn.

- **Nghiệp vụ Frontend**:
  - Thêm bộ nút chọn kích thước và bảng màu trên trang chi tiết sản phẩm.
  - Tự động hiển thị số lượng tồn kho tương ứng khi khách bấm chọn từng biến thể.

---

## Kịch Bản 3: Sổ Địa Chỉ Nhận Hàng Và Tính Phí Vận Chuyển Tự Động

- **Mục tiêu**: Giúp khách hàng không phải nhập lại địa chỉ mỗi lần mua sắm và hiển thị phí vận chuyển rõ ràng trước khi thanh toán.

- **Nghiệp vụ Backend**:
  - Cho phép người dùng lưu nhiều địa chỉ nhận hàng (nhà riêng, công ty) và chọn một địa chỉ mặc định.
  - Tích hợp danh mục chuẩn Tỉnh/Thành phố, Quận/Huyện, Phường/Xã.
  - Xây dựng bảng giá cước vận chuyển tự động theo từng khu vực địa lý.
  - Hỗ trợ chính sách miễn phí vận chuyển cho các đơn hàng đạt mốc giá trị quy định.

- **Nghiệp vụ Frontend**:
  - Trang quản lý danh sách địa chỉ trong phần cài đặt tài khoản cá nhân.
  - Menu thả xuống cho phép chọn nhanh địa chỉ đã lưu ngay tại trang thanh toán.

---

## Kịch Bản 4: Thông Báo Trực Tiếp Trên Web Qua WebSocket (In-App Notification)

- **Mục tiêu**: Thông báo trạng thái đơn hàng và các chương trình khuyến mãi ngay tức thì trên giao diện web mà không cần người dùng tải lại trang.

- **Nghiệp vụ Backend**:
  - Thiết lập kết nối WebSocket với giao thức STOMP tại Notification Service.
  - Mỗi khi trạng thái đơn hàng thay đổi (đã duyệt, đang giao, đã giao thành công), hệ thống tự động gửi thông báo trực tiếp đến tài khoản đó.
  - Lưu trữ danh sách thông báo vào cơ sở dữ liệu để người dùng xem lại lịch sử.

- **Nghiệp vụ Frontend**:
  - Biểu tượng hình quả chuông trên thanh điều hướng đầu trang kèm số lượng thông báo chưa đọc.
  - Bảng danh sách thông báo thả xuống hiển thị chi tiết nội dung và thời gian khi bấm vào chuông.
  - Đánh dấu đã đọc khi người dùng bấm xem thông báo.

---

## Kịch Bản 5: Mở Rộng Cổng Thanh Toán Đa Kênh (MoMo / ZaloPay / Quy Trình Đối Soát COD)

- **Mục tiêu**: Bổ sung ví điện tử phổ biến tại Việt Nam và hoàn thiện quy trình đối soát tiền COD cho Shipper.

- **Nghiệp vụ Backend**:
  - Tích hợp thêm cổng thanh toán MoMo / ZaloPay qua API Sandbox bên cạnh VNPay và COD hiện có.
  - Bổ sung chức năng xác nhận thu tiền COD: khi Shipper giao hàng thành công, hệ thống cho phép cập nhật trạng thái thanh toán từ `PENDING` sang `PAID` và tạo biên bản đối soát doanh thu.
  - Xử lý hoàn tiền tự động (Refund) khi đơn hàng online bị hủy.

- **Nghiệp vụ Frontend**:
  - Bổ sung tùy chọn ví MoMo / ZaloPay tại trang Checkout.
  - Cung cấp giao diện xác nhận "Đã thu tiền COD" trong trang Quản lý đơn hàng dành cho Admin.

---

## Kịch Bản 6: Danh Sách Sản Phẩm Yêu Thích (Wishlist)

- **Mục tiêu**: Cho phép khách hàng lưu lại những món đồ ưa thích để theo dõi giá và mua lại sau này.

- **Nghiệp vụ Backend**:
  - API thêm sản phẩm vào danh sách yêu thích, xóa sản phẩm và lấy toàn bộ danh sách.
  - Lưu dữ liệu bằng cơ sở dữ liệu hoặc Redis Set theo mã người dùng.

- **Nghiệp vụ Frontend**:
  - Nút biểu tượng trái tim trên từng thẻ sản phẩm ở trang chủ, trang danh mục và trang chi tiết sản phẩm.
  - Trang riêng hiển thị toàn bộ các sản phẩm đã yêu thích trong khu vực tài khoản cá nhân.

---

## Kịch Bản 7: Tích Hợp AI Tư Vấn Bán Hàng Và Tra Cứu Đơn Hàng (Gemini AI)

- **Mục tiêu**: Hỗ trợ giải đáp thắc mắc và tư vấn cho khách hàng tự động 24/7.

- **Nghiệp vụ Chức Năng**:
  - Tư vấn chọn kích cỡ quần áo dựa trên chiều cao và cân nặng do khách hàng cung cấp.
  - Gợi ý sản phẩm phù hợp theo phong cách và mức giá khách hàng yêu cầu.
  - Tự động tra cứu tiến độ xử lý đơn hàng khi khách cung cấp mã đơn hàng.

- **Nghiệp vụ Frontend**:
  - Cửa sổ chatbox nhỏ gọn đặt ở góc dưới cùng bên phải màn hình website.

---

## Kịch Bản 8: Nâng Cấp Kỹ Thuật Microservices (Khả Năng Chịu Lỗi Và Giám Sát)

- **Truy vết phân tán (Distributed Tracing)**:
  - Cài đặt Micrometer Tracing và Zipkin để theo dõi toàn bộ đường đi của một yêu cầu từ Gateway, qua các service và sự kiện Kafka.
  - Dễ dàng xác định chính xác dịch vụ nào đang bị chậm hoặc gây lỗi.

- **Chống lỗi lan truyền (Circuit Breaker)**:
  - Cấu hình Resilience4j trên API Gateway và các FeignClient giữa các service.
  - Tự động kích hoạt cơ chế dự phòng (fallback) khi một service phụ gặp sự cố, đảm bảo luồng đặt hàng chính không bị sập theo.

- **Đảm bảo tính nhất quán dữ liệu (Transactional Outbox Pattern)**:
  - Lưu dữ liệu đơn hàng và lưu sự kiện cần gửi vào cùng một transaction trong cơ sở dữ liệu.
  - Dùng tiến trình quét để đẩy dữ liệu vào Kafka, tránh tình trạng lưu database thành công nhưng gửi message thất bại.

- **Bảng điều khiển giám sát hệ thống (Prometheus & Grafana)**:
  - Thu thập các chỉ số vận hành gồm: mức sử dụng CPU, dung lượng RAM, số lượng yêu cầu mỗi giây, độ trễ API và độ trễ tiêu thụ message Kafka.
  - Hiển thị trực quan toàn bộ hệ thống trên một màn hình quản trị duy nhất.
