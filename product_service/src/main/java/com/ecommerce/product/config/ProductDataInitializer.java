package com.ecommerce.product.config;

import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.ecommerce.product.service.ProductService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductDataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductService productService;

    @Override
    public void run(String... args) throws Exception {
        Map<String, Category> categoryMap = seedCategories();
        seedInitialProducts(categoryMap);
        seedAdditional20Products(categoryMap);

        // syncProduct() bên Inventory là idempotent nên chạy lại mỗi lần khởi động là an toàn,
        // đồng thời tự vá những bản ghi kho bị thiếu nếu event từng thất lạc.
        productService.republishCatalog();
    }

    private Map<String, Category> seedCategories() {
        List<Category> defaultCategories = List.of(
            Category.builder().name("Điện thoại").description("Điện thoại di động thông minh các thương hiệu").build(),
            Category.builder().name("Laptop").description("Máy tính xách tay cao cấp cho công việc và giải trí").build(),
            Category.builder().name("Phụ kiện âm thanh").description("Tai nghe, loa Bluetooth chất lượng cao").build(),
            Category.builder().name("Phụ kiện máy tính").description("Chuột, bàn phím và thiết bị ngoại vi").build(),
            Category.builder().name("Đồng hồ thông minh").description("Smartwatch theo dõi sức khỏe và thể thao").build(),
            Category.builder().name("Máy tính bảng").description("Tablet màn hình lớn cho công việc và giải trí").build(),
            Category.builder().name("Camera").description("Máy ảnh chuyên nghiệp và Action Cam").build(),
            Category.builder().name("Tivi").description("Smart TV 4K, OLED, QLED màn hình siêu nét").build()
        );

        Map<String, Category> map = new HashMap<>();
        for (Category cat : defaultCategories) {
            Category saved = categoryRepository.findByName(cat.getName())
                    .orElseGet(() -> categoryRepository.save(cat));
            map.put(saved.getName(), saved);
        }
        log.info("Khởi tạo {} danh mục thành công vào product_db!", map.size());
        return map;
    }

    private void seedInitialProducts(Map<String, Category> categoryMap) {
        if (productRepository.count() == 0) {
            log.info("Khởi tạo dữ liệu sản phẩm mẫu ban đầu vào product_db...");

            List<Product> saved = productRepository.saveAll(List.of(
                Product.builder()
                    .sku("IP15PM-256-TI")
                    .name("iPhone 15 Pro Max 256GB Titan Tự Nhiên")
                    .description("Điện thoại thông minh Apple iPhone 15 Pro Max khung Titan cao cấp, chip A17 Pro mạnh mẽ.")
                    .price(new BigDecimal("29990000"))
                    .category(categoryMap.get("Điện thoại"))
                    .imageUrl("https://placehold.co/400x400?text=iPhone15ProMax")
                    .status("ACTIVE")
                    .build(),

                Product.builder()
                    .sku("MBP-M3PRO-16")
                    .name("MacBook Pro 16 inch M3 Pro 18GB/512GB")
                    .description("Laptop Apple MacBook Pro 16 inch chip M3 Pro hiệu năng vượt trội cho lập trình viên và thiết kế.")
                    .price(new BigDecimal("49990000"))
                    .category(categoryMap.get("Laptop"))
                    .imageUrl("https://placehold.co/400x400?text=MacBookProM3")
                    .status("ACTIVE")
                    .build(),

                Product.builder()
                    .sku("SGS24U-512-GY")
                    .name("Samsung Galaxy S24 Ultra 12GB/512GB")
                    .description("Super Smartphone Samsung tích hợp Galaxy AI thông minh, bút S-Pen và camera 200MP.")
                    .price(new BigDecimal("27990000"))
                    .category(categoryMap.get("Điện thoại"))
                    .imageUrl("https://placehold.co/400x400?text=S24Ultra")
                    .status("ACTIVE")
                    .build(),

                Product.builder()
                    .sku("SONY-WH1000XM5")
                    .name("Tai nghe Bluetooth Chống ồn Sony WH-1000XM5")
                    .description("Tai nghe chụp tai chống ồn chủ động hàng đầu thế giới với thời lượng pin lên đến 30 giờ.")
                    .price(new BigDecimal("8490000"))
                    .category(categoryMap.get("Phụ kiện âm thanh"))
                    .imageUrl("https://placehold.co/400x400?text=SonyWH1000XM5")
                    .status("ACTIVE")
                    .build()
            ));

            log.info("Seed {} sản phẩm ban đầu thành công!", saved.size());
        }
    }

    private void seedAdditional20Products(Map<String, Category> categoryMap) {
        List<String> existingSkus = List.of(
            "IP14-128-BL", "SGS23-256-WH", "OPPO-R17PRO", "XIAO-14U-BK",
            "DELL-XPS15-I7", "ASUS-ZENBOOK14", "HP-ENVY16-I5", "LOGI-MX-MASTER3",
            "APPLE-WATCH-S9", "SAMSUNG-WATCH6", "GARMIN-FENIX7", "IPAD-PRO11-M4",
            "SAMSUNG-TAB-S9", "AMAZON-FIRE10", "GOPRO-HERO12", "FUJI-XT5",
            "BOSE-QC45", "JBLL-FLIP6", "LG-4K-OLED55", "SAMSUNG-QN85"
        );

        long newCount = existingSkus.stream()
            .filter(sku -> productRepository.findBySku(sku).isEmpty())
            .count();

        if (newCount == 0) {
            log.info("20 sản phẩm bổ sung đã tồn tại, bỏ qua seed.");
            return;
        }

        log.info("Đang seed {} sản phẩm bổ sung mới...", newCount);

        List<Product> additional = List.of(
            // ===== ĐIỆN THOẠI =====
            Product.builder().sku("IP14-128-BL").name("iPhone 14 128GB Màu Xanh Dương")
                .description("iPhone 14 chip A15 Bionic, camera 12MP OIS, pin 3279 mAh, hỗ trợ 5G.")
                .price(new BigDecimal("18990000")).category(categoryMap.get("Điện thoại"))
                .imageUrl("https://placehold.co/400x400?text=iPhone14").status("ACTIVE").build(),

            Product.builder().sku("SGS23-256-WH").name("Samsung Galaxy S23 8GB/256GB Kem")
                .description("Samsung S23 chip Snapdragon 8 Gen 2, camera 50MP, màn hình AMOLED 6.1 inch.")
                .price(new BigDecimal("16990000")).category(categoryMap.get("Điện thoại"))
                .imageUrl("https://placehold.co/400x400?text=GalaxyS23").status("ACTIVE").build(),

            Product.builder().sku("OPPO-R17PRO").name("OPPO Reno11 Pro 5G 12GB/256GB")
                .description("OPPO Reno11 Pro chip Dimensity 8200, camera telephoto 64MP, sạc nhanh 80W SUPERVOOC.")
                .price(new BigDecimal("12990000")).category(categoryMap.get("Điện thoại"))
                .imageUrl("https://placehold.co/400x400?text=OPPOReno11Pro").status("ACTIVE").build(),

            Product.builder().sku("XIAO-14U-BK").name("Xiaomi 14 Ultra 16GB/512GB Đen")
                .description("Xiaomi 14 Ultra Leica camera 1 inch, chip Snapdragon 8 Gen 3, sạc 90W HyperCharge.")
                .price(new BigDecimal("23990000")).category(categoryMap.get("Điện thoại"))
                .imageUrl("https://placehold.co/400x400?text=Xiaomi14Ultra").status("ACTIVE").build(),

            // ===== LAPTOP =====
            Product.builder().sku("DELL-XPS15-I7").name("Dell XPS 15 Intel Core i7-13700H 32GB/1TB")
                .description("Dell XPS 15 màn hình OLED 3.5K, RTX 4060, pin 86Whr, thiết kế nhôm cao cấp.")
                .price(new BigDecimal("42990000")).category(categoryMap.get("Laptop"))
                .imageUrl("https://placehold.co/400x400?text=DellXPS15").status("ACTIVE").build(),

            Product.builder().sku("ASUS-ZENBOOK14").name("ASUS Zenbook 14 OLED Ryzen 7 16GB/512GB")
                .description("ASUS Zenbook 14 màn hình 2.8K OLED, pin 75Whr, trọng lượng chỉ 1.39kg, cổng USB-C Thunderbolt 4.")
                .price(new BigDecimal("22990000")).category(categoryMap.get("Laptop"))
                .imageUrl("https://placehold.co/400x400?text=ZenbookOLED").status("ACTIVE").build(),

            Product.builder().sku("HP-ENVY16-I5").name("HP Envy 16 Intel Core i5-13500H 16GB/512GB")
                .description("HP Envy 16 màn hình 2.5K 120Hz, RTX 4050, âm thanh Bang & Olufsen.")
                .price(new BigDecimal("28990000")).category(categoryMap.get("Laptop"))
                .imageUrl("https://placehold.co/400x400?text=HPEnvy16").status("ACTIVE").build(),

            // ===== PHỤ KIỆN MÁY TÍNH =====
            Product.builder().sku("LOGI-MX-MASTER3").name("Chuột Logitech MX Master 3S Không dây")
                .description("Chuột không dây Logitech MX Master 3S, DPI 8000, kết nối Bluetooth và USB-C, cuộn bánh xe MagSpeed.")
                .price(new BigDecimal("2190000")).category(categoryMap.get("Phụ kiện máy tính"))
                .imageUrl("https://placehold.co/400x400?text=MXMaster3S").status("ACTIVE").build(),

            // ===== ĐỒNG HỒ THÔNG MINH =====
            Product.builder().sku("APPLE-WATCH-S9").name("Apple Watch Series 9 GPS 45mm Nhôm")
                .description("Apple Watch Series 9 chip S9, màn hình Always-On Retina, Double Tap gesture, theo dõi sức khỏe toàn diện.")
                .price(new BigDecimal("10990000")).category(categoryMap.get("Đồng hồ thông minh"))
                .imageUrl("https://placehold.co/400x400?text=AppleWatchS9").status("ACTIVE").build(),

            Product.builder().sku("SAMSUNG-WATCH6").name("Samsung Galaxy Watch6 Classic 47mm Đen")
                .description("Samsung Watch6 Classic viền xoay vật lý, Wear OS, theo dõi ECG, SpO2, nặng chỉ 59g.")
                .price(new BigDecimal("8490000")).category(categoryMap.get("Đồng hồ thông minh"))
                .imageUrl("https://placehold.co/400x400?text=Watch6Classic").status("ACTIVE").build(),

            Product.builder().sku("GARMIN-FENIX7").name("Đồng hồ thể thao Garmin Fenix 7 Solar GPS")
                .description("Garmin Fenix 7 Solar kính cường lực sạc năng lượng mặt trời, pin 28 ngày, GPS đa tần, chống nước 10ATM.")
                .price(new BigDecimal("18990000")).category(categoryMap.get("Đồng hồ thông minh"))
                .imageUrl("https://placehold.co/400x400?text=GarminFenix7").status("ACTIVE").build(),

            // ===== MÁY TÍNH BẢNG =====
            Product.builder().sku("IPAD-PRO11-M4").name("iPad Pro 11 inch M4 256GB WiFi")
                .description("iPad Pro 11 inch chip M4, màn hình OLED tandem siêu mỏng 5.1mm, Apple Pencil Pro.")
                .price(new BigDecimal("24990000")).category(categoryMap.get("Máy tính bảng"))
                .imageUrl("https://placehold.co/400x400?text=iPadProM4").status("ACTIVE").build(),

            Product.builder().sku("SAMSUNG-TAB-S9").name("Samsung Galaxy Tab S9 12GB/256GB WiFi")
                .description("Samsung Tab S9 chip Snapdragon 8 Gen 2, màn hình Dynamic AMOLED 2X 120Hz, đi kèm S-Pen.")
                .price(new BigDecimal("18990000")).category(categoryMap.get("Máy tính bảng"))
                .imageUrl("https://placehold.co/400x400?text=TabS9").status("ACTIVE").build(),

            Product.builder().sku("AMAZON-FIRE10").name("Amazon Fire HD 10 Plus 64GB WiFi 2023")
                .description("Máy tính bảng Amazon Fire HD 10 màn hình Full HD 10.1 inch, 4GB RAM, pin 12 giờ, sạc không dây 15W.")
                .price(new BigDecimal("4990000")).category(categoryMap.get("Máy tính bảng"))
                .imageUrl("https://placehold.co/400x400?text=FireHD10").status("ACTIVE").build(),

            // ===== CAMERA =====
            Product.builder().sku("GOPRO-HERO12").name("GoPro HERO12 Black Action Camera 5.3K")
                .description("GoPro HERO12 Black quay 5.3K/60fps, chống rung HyperSmooth 6.0, chống nước 10m, pin Enduro 154 phút.")
                .price(new BigDecimal("9990000")).category(categoryMap.get("Camera"))
                .imageUrl("https://placehold.co/400x400?text=GoPro12").status("ACTIVE").build(),

            Product.builder().sku("FUJI-XT5").name("Fujifilm X-T5 Body Only (Không kèm ống kính)")
                .description("Fujifilm X-T5 cảm biến 40.2MP X-Trans BSI CMOS 5, IBIS 7 chặn, quay 6.2K RAW, thiết kế retro cao cấp.")
                .price(new BigDecimal("43990000")).category(categoryMap.get("Camera"))
                .imageUrl("https://placehold.co/400x400?text=FujiXT5").status("ACTIVE").build(),

            // ===== ÂM THANH =====
            Product.builder().sku("BOSE-QC45").name("Tai nghe Bose QuietComfort 45 Không dây")
                .description("Bose QC45 chống ồn chủ động, pin 24 giờ, cổng USB-C, chế độ Aware Mode, siêu nhẹ 240g.")
                .price(new BigDecimal("7490000")).category(categoryMap.get("Phụ kiện âm thanh"))
                .imageUrl("https://placehold.co/400x400?text=BoseQC45").status("ACTIVE").build(),

            Product.builder().sku("JBLL-FLIP6").name("Loa Bluetooth JBL Flip 6 Chống nước IPX7")
                .description("JBL Flip 6 âm thanh stereo 20W, pin 12 giờ, chống nước IPX7, kết nối PartyBoost.")
                .price(new BigDecimal("2490000")).category(categoryMap.get("Phụ kiện âm thanh"))
                .imageUrl("https://placehold.co/400x400?text=JBLFlip6").status("ACTIVE").build(),

            // ===== TIVI =====
            Product.builder().sku("LG-4K-OLED55").name("Tivi LG OLED 55 inch 4K Smart TV C3 2023")
                .description("LG OLED C3 55 inch α9 Gen6 AI Processor, Dolby Vision IQ, NVIDIA G-Sync, 4 cổng HDMI 2.1.")
                .price(new BigDecimal("29990000")).category(categoryMap.get("Tivi"))
                .imageUrl("https://placehold.co/400x400?text=LGOLED55C3").status("ACTIVE").build(),

            Product.builder().sku("SAMSUNG-QN85").name("Tivi Samsung Neo QLED 85 inch 4K QN85C 2023")
                .description("Samsung Neo QLED 85 inch Quantum Mini LED, 144Hz, Dolby Atmos, OTS+, Game Mode Pro.")
                .price(new BigDecimal("64990000")).category(categoryMap.get("Tivi"))
                .imageUrl("https://placehold.co/400x400?text=SamsungQN85").status("ACTIVE").build()
        );

        List<Product> toSave = additional.stream()
            .filter(p -> productRepository.findBySku(p.getSku()).isEmpty())
            .toList();

        if (!toSave.isEmpty()) {
            productRepository.saveAll(toSave);
            log.info("Seed bổ sung {} sản phẩm mới thành công vào product_db!", toSave.size());
        }
    }
}
