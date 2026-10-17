# BÁO CÁO KẾT QUẢ KIỂM THỬ BACKEND - KỊCH BẢN 1

> **Kịch bản 1**: Hệ Thống Đánh Giá, Nhận Xét, Lượt Đã Bán & Tặng Thưởng Xu (Review, Rating, Total Sold & Gamification)  
> **Ngày thực hiện**: 24/09/2026  
> **Công cụ kiêm thử**: JUnit 5, Mockito, Spring Boot Test, Maven Test Runner  
> **Tổng số Test Cases**: **21/21 Pass (100% Thành công)**

---

## 1. TỔNG QUAN KẾT QUẢ KIỂM THỬ (TEST SUMMARY)

| Microservice | Lớp Kiểm Thử (Test Class) | Số Case | Thành Công (Pass) | Thất Bại (Fail) | Tỷ Lệ |
| :--- | :--- | :---: | :---: | :---: | :---: |
| `product_service` | `ReviewServiceImplTest` | 10 | 10 | 0 | 100% |
| `product_service` | `OrderEventListenerTest` | 2 | 2 | 0 | 100% |
| `order_service` | `OrderServiceImplTest` | 6 | 6 | 0 | 100% |
| `auth_service` | `RewardEventListenerTest` | 3 | 3 | 0 | 100% |
| **TỔNG CỘNG** | **3 Microservices** | **21** | **21** | **0** | **100%** |

---

## 2. CHI TIẾT CÁC CASE KIỂM THỬ THEO TỪNG MODULE

### 2.1. Module `product_service` (12 Test Cases)

#### Lớp: `ReviewServiceImplTest`
| STT | Mã Case | Tên Case Kiểm Thử | Trạng Thái | Mô Tả & Kết Quả Mong Đợi |
| :---: | :--- | :--- | :---: | :--- |
| 1 | **TC-PS-01** | Đánh giá >= 50 ký tự + 0 ảnh chụp | **PASS** | `coinsAwarded = 100`, phát event `REVIEW_REWARD_GRANTED` (100 Xu), cập nhật `avgRating` & xóa Redis Cache. |
| 2 | **TC-PS-02** | Đánh giá >= 50 ký tự + Có ảnh thực tế | **PASS** | `coinsAwarded = 200`, lưu `ReviewImage` Cloudinary, phát event `REVIEW_REWARD_GRANTED` (200 Xu). |
| 3 | **TC-PS-03** | Đánh giá < 50 ký tự | **PASS** | Lưu review nhưng `coinsAwarded = 0`, không phát event tặng Xu. |
| 4 | **TC-PS-04** | Đã đánh giá món hàng này trước đó | **PASS** | Quăng `IllegalStateException` ("Sản phẩm này trong đơn hàng của bạn đã được đánh giá rồi!"). |
| 5 | **TC-PS-05** | Xác minh đơn không đủ điều kiện | **PASS** | Quăng `IllegalStateException` khi `OrderClient.verifyPurchase` trả về `eligible = false`. |
| 6 | **TC-PS-06** | Sửa đánh giá lần 1 trong 7 ngày | **PASS** | Cập nhật sao và nhận xét thành công, gắn cờ `isEdited = true`. |
| 7 | **TC-PS-07** | Từ chối sửa đánh giá lần thứ 2 | **PASS** | Quăng `IllegalStateException` ("Mỗi bài đánh giá chỉ được phép chỉnh sửa 1 lần duy nhất!"). |
| 8 | **TC-PS-08** | Từ chối sửa đánh giá sau 7 ngày | **PASS** | Quăng `IllegalStateException` ("Đã quá thời hạn 7 ngày..."). |
| 9 | **TC-PS-09** | Admin ẩn bài đánh giá vi phạm | **PASS** | Chuyển `isHidden = true`, tính toán lũy tiến loại điểm sao bị ẩn khỏi `avgRating` của sản phẩm. |
| 10 | **TC-PS-10** | Admin viết câu trả lời cho đánh giá | **PASS** | Lưu `shopReply` và thời điểm `shopRepliedAt`. |

#### Lớp: `OrderEventListenerTest` (Kafka Consumer)
| STT | Mã Case | Tên Case Kiểm Thử | Trạng Thái | Mô Tả & Kết Quả Mong Đợi |
| :---: | :--- | :--- | :---: | :--- |
| 11 | **TC-PS-11** | Nhận event `ORDER_DELIVERED` | **PASS** | Cộng dồn `totalSold = totalSold + quantity`, làm mới Redis Cache sản phẩm. |
| 12 | **TC-PS-12** | Nhận event `ORDER_REFUNDED` | **PASS** | Trừ `totalSold = GREATEST(0, totalSold - quantity)`, tự động ẩn bài đánh giá (`isHidden = true`), cập nhật lại `avgRating` & xóa Redis Cache. |

---

### 2.2. Module `order_service` (6 Test Cases)

#### Lớp: `OrderServiceImplTest`
| STT | Mã Case | Tên Case Kiểm Thử | Trạng Thái | Mô Tả & Kết Quả Mong Đợi |
| :---: | :--- | :--- | :---: | :--- |
| 13 | **TC-OS-01** | Verify Purchase hợp lệ | **PASS** | Trả về `eligible = true` với `orderItemId`, `productId`, `userId` khi đơn đạt trạng thái `DELIVERED` trong 30 ngày. |
| 14 | **TC-OS-02** | Verify Purchase đơn chưa giao | **PASS** | Trả về `eligible = false` với thông điệp "Đơn hàng chưa được giao thành công!". |
| 15 | **TC-OS-03** | Verify Purchase quá 30 ngày | **PASS** | Trả về `eligible = false` với thông điệp "Đã quá thời hạn 30 ngày...". |
| 16 | **TC-OS-04** | Đặt hàng dùng Xu tích lũy (Checkout) | **PASS** | Trừ tiền bill (`totalAmount = subtotal - coinsUsed`), lưu `coinsUsed`, phát sự kiện `CoinsRedeemedEvent` qua Kafka `coins-redeemed-topic`. |
| 17 | **TC-OS-05** | Admin refund (`DELIVERED -> REFUNDED`) | **PASS** | Đơn chuyển trạng thái `REFUNDED`, phát sự kiện `ORDER_REFUNDED` qua Kafka topic `order-refunded-topic`. |
| 18 | **TC-OS-06** | Khách hủy đơn hàng có dùng Xu | **PASS** | Đơn chuyển `CANCELLED`, phát sự kiện `CoinsRefundedEvent` hoàn Xu qua Kafka topic `coins-refunded-topic`. |

---

### 2.3. Module `auth_service` (3 Test Cases)

#### Lớp: `RewardEventListenerTest` (Kafka Consumer)
| STT | Mã Case | Tên Case Kiểm Thử | Trạng Thái | Mô Tả & Kết Quả Mong Đợi |
| :---: | :--- | :--- | :---: | :--- |
| 19 | **TC-AS-01** | Lắng nghe topic `reward-topic` | **PASS** | Nhận `REVIEW_REWARD_GRANTED`, cộng dồn xu thưởng vào ví user (`reward_coins = reward_coins + coins`). |
| 20 | **TC-AS-02** | Lắng nghe topic `coins-redeemed-topic` | **PASS** | Nhận `COINS_REDEEMED`, khấu trừ xu ví user (`reward_coins = reward_coins - coinsUsed`). |
| 21 | **TC-AS-03** | Lắng nghe topic `coins-refunded-topic` | **PASS** | Nhận `COINS_REFUNDED`, hoàn lại xu ví user (`reward_coins = reward_coins + coinsRefunded`). |

---

## 3. LỖI ĐÃ PHÁT HIỆN & ĐÃ SỬA CHỮA KHI CHẠY SUITE KIỂM THỬ

Trong quá trình thực thi bộ kiểm thử tự động, 1 lỗi Logic Backend đã được phát hiện và xử lý ngay lập tức:

> **Lỗi phát hiện**: Tại `OrderServiceImpl.java` (phương thức `publishEvent`), khi chuyển đơn hàng sang trạng thái `REFUNDED`, câu lệnh điều kiện kiểm tra chuỗi `eventType` là `if ("REFUNDED".equalsIgnoreCase(eventType))`, trong khi sự kiện truyền vào là `"ORDER_REFUNDED"`. Việc này khiến sự kiện `ORDER_REFUNDED` chỉ được phát lên `order-topic` mà bỏ sót việc phát lên `order-refunded-topic`.
> 
> **Khắc phục**: Đã cập nhật câu lệnh điều kiện thành `if ("REFUNDED".equalsIgnoreCase(eventType) || "ORDER_REFUNDED".equalsIgnoreCase(eventType))` tại [`OrderServiceImpl.java`](file:///d:/Ecommerce-Microservice/order_service/src/main/java/com/ecommerce/order/service/impl/OrderServiceImpl.java#L416-L420). Sau khi sửa, toàn bộ 21/21 test cases đã **PASS** 100%.

---

## 4. KẾT LUẬN

Hệ thống Backend cho **Kịch Bản 1** đã hoàn thành kiểm thử đơn vị & tích hợp luồng sự kiện (Event-Driven Integration) đạt kết quả **100% PASS**. Đảm bảo đúng các nghiệp vụ:
1. Quy tắc cộng/trừ xu chuẩn xác (100 Xu / 200 Xu, khấu trừ khi checkout, hoàn xu khi hủy đơn).
2. Quy tắc ràng buộc thời gian (đánh giá trong 30 ngày, sửa 1 lần trong 7 ngày, khóa đánh giá lại khi hoàn trả hàng).
3. Luồng sự kiện Kafka đồng bộ chỉ số lượt bán `total_sold`, điểm sao `avg_rating`, số dư ví xu `reward_coins` và làm mới Redis Cache giữa các dịch vụ.
