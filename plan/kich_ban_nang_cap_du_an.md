# KỊCH BẢN NÂNG CẤP DỰ ÁN E-COMMERCE MICROSERVICES

---

## Kịch Bản 1: Hệ Thống Đánh Giá, Nhận Xét, Lượt Đã Bán & Tặng Thưởng Xu (Review, Rating, Total Sold & Gamification)

- **Mục tiêu**: Tăng độ uy tín của sản phẩm và kích thích khách hàng để lại đánh giá chất lượng bằng cơ chế tặng xu (Gamification). Cho phép xem số lượt mua thực tế, thông tin phân loại hàng đã mua (Size, Màu) và chỉ người thực sự nhận hàng thành công mới được đánh giá.

- **Nghiệp vụ Backend**:
  - Gộp trực tiếp module Review vào `product_service` để tận dụng lưu trữ Cloudinary và đồng bộ bộ đệm Redis.
  - Cập nhật bảng `products` bổ sung 3 trường: `avg_rating` (điểm sao trung bình), `total_reviews` (tổng lượt đánh giá), và `total_sold` (tổng số lượng sản phẩm đã bán).
  - Bảng `reviews` lưu thêm:
    - `order_item_id`: Mã chi tiết sản phẩm trong đơn hàng (dùng làm khóa chính xác minh).
    - `variant_info`: Ghi nhận phân loại hàng khách đã mua (ví dụ: "Màu Đen, Size L").
    - `coins_awarded`: Số xu đã thưởng cho bài đánh giá này (100 Xu hoặc 200 Xu).
    - `is_hidden`: Trạng thái ẩn/hiện của bài đánh giá (dành cho Admin hoặc đơn bị Trả hàng).
  - **Cơ chế Tặng thưởng Xu & Quy đổi Sử dụng (Gamification via Kafka)**:
    - **Giá trị quy đổi**: **1 Xu = 1 VNĐ**.
    - **Mức thưởng tích lũy**:
      - Nhận xét chữ (>= 50 ký tự): Thưởng **100 Xu** (tương đương **100 VNĐ**).
      - Nhận xét chữ (>= 50 ký tự) + Đính kèm ảnh chụp thực tế (tối đa 5 ảnh qua Cloudinary): Thưởng **200 Xu** (tương đương **200 VNĐ**).
      - Nhận xét dưới 50 ký tự: Vẫn lưu bài đánh giá nhưng **không tặng Xu** (`coins_awarded = 0`).
    - `product_service` phát sự kiện `REVIEW_REWARD_GRANTED` qua Kafka topic `reward-topic`.
    - `auth_service` lắng nghe sự kiện để cộng dồn vào ví xu của người dùng (`users.reward_coins = reward_coins + coins`).
  - **Cơ chế Áp dụng Xu giảm giá trực tiếp / Đổi Mã giảm giá khi Đặt hàng (Checkout)**:
    - Người dùng tích cực đánh giá để tích lũy Xu trong tài khoản cá nhân.
    - Tại trang Thanh Toán (Checkout), hệ thống bổ sung công tắc: **`[x] Sử dụng Xu tích lũy (Hiện có: X Xu = X VNĐ)`**.
    - Khi chọn sử dụng Xu: Tổng tiền thanh toán được trừ đi số tiền tương ứng với số Xu áp dụng (`tổng_thanh_toán = tổng_tiền_đơn - số_xu_dùng`). Số Xu còn lại có thể đổi thành Voucher/Mã giảm giá qua `promotion_service`.
    - Khi đơn hàng tạo thành công, `order_service` gửi thông điệp trừ số Xu đã dùng khỏi ví người dùng (`users.reward_coins = reward_coins - coins_used`).
    - Nếu đơn hàng bị Hủy trước khi giao (`CANCELLED`), hệ thống tự động hoàn trả lại số Xu đã khấu trừ về ví cho người dùng.
  - **Giới hạn thời hạn & Chỉnh sửa đánh giá**:
    - **Thời hạn đánh giá**: Chỉ cho phép gửi đánh giá trong vòng **30 ngày** kể từ khi đơn hàng chuyển sang trạng thái `DELIVERED`. Sau 30 ngày, hệ thống tự động khóa nút đánh giá.
    - **Chỉnh sửa đánh giá**: Cho phép người dùng chỉnh sửa nội dung bài đánh giá **1 lần duy nhất** trong vòng **7 ngày** kể từ ngày gửi đánh giá (không phát thêm Xu khi chỉnh sửa).
  - **Chuẩn hóa vòng đời và cơ chế cộng/trừ lượt mua (`total_sold`) qua Kafka**:
    - **Chỉ tính khi giao hàng thành công**: Khi đơn hàng chuyển sang trạng thái `DELIVERED`, `order_service` phát sự kiện `ORDER_DELIVERED` kèm danh sách sản phẩm và số lượng mua (`quantity`). `product_service` nhận sự kiện để cộng dồn: `total_sold = total_sold + quantity`.
    - **Xử lý hoàn hàng / trả hàng (`REFUNDED`)**: `order_service` mở rộng trạng thái cho phép chuyển từ `DELIVERED -> REFUNDED` khi khách trả hàng thành công và phát sự kiện `ORDER_REFUNDED`.
    - `product_service` lắng nghe `ORDER_REFUNDED` để trừ lại số lượng `total_sold = GREATEST(0, total_sold - quantity)`, đồng thời tự động ẩn bài đánh giá liên quan (`is_hidden = true`) và tính toán lại `avg_rating` (không cần thu hồi Xu đã tặng).
    - `inventory_service` lắng nghe `ORDER_REFUNDED` để hoàn lại số lượng tồn kho.
    - Cả hai sự kiện đều tự động xóa cache Redis sản phẩm để giao diện cập nhật tức thì.
  - **Quy tắc xác minh mua hàng nghiêm ngặt (Verified Purchase)**:
    - `order_service` cung cấp API nội bộ `GET /api/v1/orders/verify-purchase`.
    - Điều kiện được đánh giá: Đúng `userId`, đúng mã đơn `orderId`, đơn đã đạt trạng thái `DELIVERED` (chưa bị `REFUNDED`), và có chứa `order_item_id`.
    - Ràng buộc duy nhất `UNIQUE(order_item_id)` để từng món hàng thuộc từng biến thể cụ thể (Size/Màu) trong đơn chỉ được đánh giá đúng 1 lần (xử lý triệt để trường hợp mua nhiều biến thể của cùng 1 sản phẩm trong 1 đơn).
  - **Tối ưu hóa tính điểm sao trung bình (`avg_rating`)**:
    - Cập nhật điểm `avg_rating` và `total_reviews` theo công thức lũy tiến khi có bài đánh giá mới hoặc khi bài đánh giá bị ẩn/hiện, tránh việc quét lại toàn bộ cơ sở dữ liệu (`SELECT AVG`).
  - **Quản trị viên (Admin)**:
    - Admin có quyền xem toàn bộ đánh giá, ẩn/hiện các đánh giá vi phạm (khi ẩn sẽ tự động loại số sao khỏi điểm trung bình của sản phẩm) và viết phản hồi chính thức cho khách hàng.

- **Nghiệp vụ Frontend**:
  - Thẻ sản phẩm và trang chi tiết sản phẩm hiển thị: Điểm sao trung bình, tổng số đánh giá và số lượng "Đã bán" (ví dụ: ⭐ 4.9 | 120 Đánh giá | Đã bán 350).
  - **Khối bình luận chi tiết tại Trang sản phẩm**:
    - Thanh biểu đồ tỷ lệ số sao (5★, 4★, 3★, 2★, 1★).
    - Bộ lọc bài đánh giá nhanh: `[Tất cả]` `[5 Sao]` `[4 Sao]` `[3 Sao]` `[2 Sao]` `[1 Sao]` `[Có Hình Ảnh]` `[Có Bình Luận]`.
    - Chi tiết từng bình luận: Tên khách đã che mờ (`nguoidung***`), ngày đánh giá, số sao, **dòng chữ "Phân loại hàng: [Size, Màu]"**, nội dung nhận xét, album ảnh thực tế, badge "Đã mua hàng" và phản hồi chính thức từ Shop.
  - Không mở ô nhập đánh giá tự do tại trang chi tiết sản phẩm.
  - Trang Lịch sử đơn hàng: Cạnh món hàng thuộc đơn `DELIVERED` (trong thời hạn 30 ngày) hiển thị nút **"Đánh giá sản phẩm (Nhận đến 200 Xu)"** để kích thích khách bấm vào.
  - Trang Quản lý đơn hàng (Admin) bổ sung nút thao tác "Xác nhận trả hàng / Hoàn tiền" (chuyển đơn `DELIVERED -> REFUNDED`).

- **Danh Sách Chức Năng Cần Nâng Cấp Chi Tiết Cho Kịch Bản 1**:

  ### 1. `product_service` (Trung tâm quản lý Đánh giá, Điểm sao & Lượt đã bán)
  * **Database**:
    * Thêm cột `avg_rating` (Double), `total_reviews` (Integer), `total_sold` (Integer) vào bảng `products`.
    * Tạo bảng `reviews`: `id`, `order_item_id` (UNIQUE), `product_id`, `user_id`, `rating` (1-5), `comment` (Text), `variant_info` (String), `coins_awarded` (Integer), `is_hidden` (Boolean), `created_at`, `updated_at`.
    * Tạo bảng `review_images`: `id`, `review_id`, `image_url`.
  * **API Controllers & Services**:
    * `POST /api/v1/reviews`: Tiếp nhận đánh giá, upload ảnh Cloudinary, gọi API verify purchase `order_service`, đếm ký tự (>= 50 ký tự -> 100 Xu, +ảnh -> 200 Xu), phát Kafka event `REVIEW_REWARD_GRANTED`, tính lũy tiến `avg_rating` & xóa Redis Cache.
    * `PUT /api/v1/reviews/{id}`: Cho phép sửa nội dung đánh giá 1 lần trong vòng 7 ngày.
    * `GET /api/v1/products/{id}/reviews`: Lấy danh sách review có phân trang, hỗ trợ filter theo sao và bài có ảnh.
    * `PUT /api/v1/admin/reviews/{id}/toggle-hide`: Admin ẩn/hiện bài vi phạm & cập nhật lại `avg_rating`.
    * `POST /api/v1/admin/reviews/{id}/reply`: Admin viết phản hồi cho đánh giá.
  * **Kafka Event Handlers**:
    * Consume `order-delivered-topic`: Cộng dồn `total_sold = total_sold + quantity` & xóa Redis Cache.
    * Consume `order-refunded-topic`: Trừ `total_sold = GREATEST(0, total_sold - quantity)`, ẩn bài đánh giá (`is_hidden = true`), cập nhật lại `avg_rating` & xóa Redis Cache.

  ### 2. `order_service` (Xác minh Mua hàng, Vòng đời Đơn & Khấu trừ Xu Checkout)
  * **API Controllers**:
    * `GET /api/v1/orders/verify-purchase`: API nội bộ kiểm tra điều kiện mua hàng (`order_item_id`, `user_id`, trạng thái `DELIVERED`, trong hạn 30 ngày).
    * `POST /api/v1/orders`: Khi người dùng bật tùy chọn dùng Xu, thực hiện tính toán khấu trừ `tổng_thanh_toán = tổng_tiền - số_xu_dùng` (1 Xu = 1 VNĐ).
  * **Kafka Publishers**:
    * Khi đơn chuyển `DELIVERED`: Phát event `ORDER_DELIVERED` lên Kafka.
    * Khi Admin bấm "Xác nhận trả hàng / Hoàn tiền" (`DELIVERED -> REFUNDED`): Phát event `ORDER_REFUNDED` lên Kafka.
    * Khi người dùng sử dụng Xu đặt hàng: Phát event `COINS_REDEEMED` để `auth_service` trừ xu trong ví.
    * Khi đơn hàng bị Hủy (`CANCELLED`): Phát event `COINS_REFUNDED` để `auth_service` hoàn lại xu.

  ### 3. `auth_service` (Quản lý Ví Xu Thưởng `reward_coins`)
  * **Database**: Bổ sung cột `reward_coins` (BigInt, default 0) vào bảng `users`.
  * **Kafka Event Handlers**:
    * Consume `reward-topic`: Nhận sự kiện `REVIEW_REWARD_GRANTED`, thực hiện cộng dồn xu (`reward_coins = reward_coins + coins`).
    * Consume `coins-redeemed-topic`: Trừ số Xu người dùng đã sử dụng khi Checkout (`reward_coins = reward_coins - coins_used`).
    * Consume `coins-refunded-topic`: Hoàn lại số Xu về ví khi đơn hàng bị Hủy (`reward_coins = reward_coins + coins_refunded`).

  ### 4. `inventory_service` (Xử lý Hoàn Kho)
  * **Kafka Event Handler**:
    * Consume `order-refunded-topic`: Nhận sự kiện `ORDER_REFUNDED`, thực hiện hoàn trả số lượng tồn kho theo mã SKU.

  ### 5. `frontend_eco` (Giao diện Khách hàng & Admin)
  * **Trang Chi tiết Sản phẩm**:
    * Hiển thị sao trung bình ⭐ `avg_rating`, số lượt đánh giá, lượt `total_sold`.
    * Khối đánh giá: Biểu đồ % số sao, bộ lọc nhanh `[Tất cả]`, `[5 Sao]`, `[Có ảnh]`, `[Có bình luận]`.
    * Chi tiết review: Tên che mờ (`nguoidung***`), badge "Đã mua hàng", dòng "Phân loại hàng: [Size, Màu]", nội dung nhận xét, album ảnh Cloudinary, phản hồi từ Shop.
  * **Trang Lịch sử Đơn hàng**:
    * Hiển thị nút **"Đánh giá sản phẩm (Nhận đến 200 Xu)"** cạnh từng món trong đơn `DELIVERED` (kèm thời gian đếm ngược 30 ngày).
    * Modal Đánh giá: Chọn sao, ô text đếm ký tự (0/50), upload ảnh thực tế.
  * **Trang Thanh Toán (Checkout)**:
    * Bổ sung công tắc: **`[x] Sử dụng Xu tích lũy (Số dư: X Xu = X VNĐ)`**.
    * Tự động tính toán số tiền được giảm giá trực tiếp vào tổng bill checkout (`1 Xu = 1 VNĐ`) hoặc quy đổi Mã giảm giá (Voucher).
  * **Trang Admin**:
    * Quản lý đơn hàng: Nút "Xác nhận trả hàng / Hoàn tiền" (`DELIVERED -> REFUNDED`).
    * Quản lý đánh giá: Danh sách review, nút Ẩn/Hiện bài vi phạm, ô nhập phản hồi của Shop.

- **Sơ Đồ Luồng Hoạt Động & Kiến Trúc (Mermaid Diagrams)**:

  ### 1. Sơ đồ Kiến trúc Tổng quan Microservices & Kafka Event-Driven
  ```mermaid
  graph TD
      subgraph Frontend["Frontend Client (Angular Web App)"]
          UI_Product["Trang Chi Tiết Sản Phẩm<br>(avg_rating | total_sold | reviews)"]
          UI_History["Trang Lịch Sử Đơn Hàng<br>(Nút 'Đánh Giá Sản Phẩm')"]
      end

      subgraph Gateway["API Gateway"]
          GW["Spring Cloud Gateway"]
      end

      subgraph Services["Microservices Backend"]
          PS["Product Service<br>(Review Module + Cloudinary + Redis)"]
          OS["Order Service<br>(Order Management + Verify Purchase)"]
          AS["Auth Service<br>(User Wallet - reward_coins)"]
          IS["Inventory Service<br>(Stock Management)"]
      end

      subgraph Storage["Databases & Cache"]
          DB_Product[("Product & Review DB")]
          DB_Order[("Order DB")]
          DB_Auth[("User DB")]
          RedisCache[("Redis Cache")]
          Cloudinary[("Cloudinary Cloud")]
      end

      subgraph EventBus["Kafka Message Broker"]
          Kafka_Reward["Topic: reward-topic"]
          Kafka_Delivered["Topic: order-delivered-topic"]
          Kafka_Refunded["Topic: order-refunded-topic"]
      end

      UI_History -->|1. Submit Review + Photos| GW
      GW --> PS
      PS -->|2. Upload Images| Cloudinary
      PS -->|3. Verify Purchase| OS
      OS --> DB_Order
      PS --> DB_Product
      PS -->|4. Invalidate Cache| RedisCache
      PS -->|5. Publish REVIEW_REWARD_GRANTED| Kafka_Reward
      Kafka_Reward --> AS
      AS --> DB_Auth

      OS -->|Publish ORDER_DELIVERED| Kafka_Delivered
      Kafka_Delivered -->|Update total_sold + Cache Clear| PS

      OS -->|Publish ORDER_REFUNDED| Kafka_Refunded
      Kafka_Refunded -->|Hide Review + Decrement total_sold| PS
      Kafka_Refunded -->|Restore Stock| IS
  ```

  ### 2. Sơ đồ Trình tự (Sequence Diagram) - Luồng Gửi Đánh Giá & Tặng Xu
  ```mermaid
  sequenceDiagram
      autonumber
      actor User as Khách Hàng
      participant FE as Frontend App
      participant Cloud as Cloudinary
      participant PS as Product Service
      participant OS as Order Service
      participant Kafka as Kafka Broker
      participant AS as Auth Service

      User->>FE: Bấm nút 'Đánh giá sản phẩm' (Nhập rating, text >= 50 ký tự, chọn ảnh)
      FE->>Cloud: Upload album ảnh chụp thực tế
      Cloud-->>FE: Trả về danh sách URL ảnh Cloudinary
      FE->>PS: POST /api/v1/reviews (order_item_id, rating, comment, photo_urls)
      PS->>OS: GET /api/v1/orders/verify-purchase (order_item_id, user_id)
      alt Không hợp lệ hoặc đã đánh giá
          OS-->>PS: 400 Bad Request / 403 Forbidden
          PS-->>FE: Báo lỗi 'Không đủ điều kiện hoặc đã đánh giá món hàng này'
      else Hợp lệ (Status = DELIVERED, UNIQUE order_item_id)
          OS-->>PS: 200 OK (Verified)
          PS->>PS: Tính toán Xu thưởng (Nội dung >= 50 ký tự + ảnh = 200 Xu)
          PS->>PS: Lưu bản ghi Review & Cập nhật lũy tiến avg_rating, total_reviews
          PS->>PS: Xóa Redis Cache của sản phẩm
          PS->>Kafka: Phát sự kiện REVIEW_REWARD_GRANTED (userId, coins=200)
          Kafka->>AS: Consumes REVIEW_REWARD_GRANTED
          AS->>AS: Cộng 200 Xu vào users.reward_coins
          PS-->>FE: 201 Created (Thành công + Thưởng 200 Xu)
          FE-->>User: Hiển thị thông báo 'Đánh giá thành công! Bạn đã nhận 200 Xu'
      end
  ```

  ### 3. Sơ đồ Trình tự (Sequence Diagram) - Luồng Giao Hàng & Trả Hàng (total_sold)
  ```mermaid
  sequenceDiagram
      autonumber
      actor Admin as Shipper / Admin
      participant OS as Order Service
      participant Kafka as Kafka Broker
      participant PS as Product Service
      participant IS as Inventory Service

      rect rgb(235, 245, 255)
          note over Admin, IS: Luồng Giao Hàng Thành Công (DELIVERED)
          Admin->>OS: Cập nhật trạng thái đơn -> DELIVERED
          OS->>Kafka: Phát sự kiện ORDER_DELIVERED (items, quantities)
          Kafka->>PS: Consume ORDER_DELIVERED
          PS->>PS: Cộng dồn total_sold = total_sold + quantity
          PS->>PS: Xóa Redis Cache sản phẩm
      end

      rect rgb(255, 240, 240)
          note over Admin, IS: Luồng Trả Hàng / Hoàn Tiền (REFUNDED)
          Admin->>OS: Xác nhận Trả hàng -> REFUNDED
          OS->>Kafka: Phát sự kiện ORDER_REFUNDED (order_id, items, quantities)
          Kafka->>PS: Consume ORDER_REFUNDED
          PS->>PS: Trừ total_sold = max(0, total_sold - quantity)
          PS->>PS: Tự động ẩn bài đánh giá (is_hidden = true) & Cập nhật lại avg_rating
          PS->>PS: Xóa Redis Cache sản phẩm
          Kafka->>IS: Consume ORDER_REFUNDED
          IS->>IS: Hoàn lại số lượng tồn kho theo mã SKU
      end
  ```

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
