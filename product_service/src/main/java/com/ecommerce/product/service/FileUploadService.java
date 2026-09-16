package com.ecommerce.product.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final Cloudinary cloudinary;

    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Tệp tin hình ảnh không được để rỗng!");
        }

        log.info("Upload Cloudinary: name={}, size={} bytes, type={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        try {
            @SuppressWarnings("rawtypes")
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", "ecommerce_products",
                    "resource_type", "image"
            ));

            String secureUrl = uploadResult.get("secure_url").toString();
            log.info("Upload thành công: {}", secureUrl);
            return secureUrl;

        } catch (IOException e) {
            log.error("Lỗi đọc file: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi đọc file ảnh: " + e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi Cloudinary: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi tải ảnh lên Cloudinary: " + e.getMessage());
        }
    }
}
