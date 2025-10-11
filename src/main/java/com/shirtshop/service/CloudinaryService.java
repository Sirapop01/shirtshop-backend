package com.shirtshop.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.shirtshop.dto.CloudinaryUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryUploadResponse uploadFile(MultipartFile file, String folderName) {
        try {
            // ⭐️ 1. ใช้วิธีใหม่ โดยการระบุ 'folder' โดยตรง
            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", folderName,          // บอก Cloudinary ว่าให้อัปโหลดลงโฟลเดอร์นี้
                    "use_filename", false,         // ไม่ใช้ชื่อไฟล์เดิม
                    "unique_filename", true,       // ให้ Cloudinary ตั้งชื่อไฟล์แบบสุ่มให้เอง (ไม่ต้องใช้ UUID แล้ว)
                    "overwrite", false             // ไม่เขียนทับไฟล์ที่มีชื่อซ้ำกัน
            ));

            return CloudinaryUploadResponse.builder()
                    .publicId(uploadResult.get("public_id").toString())
                    .url(uploadResult.get("secure_url").toString())
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("Could not upload file to Cloudinary", e);
        }
    }

    public void deleteFile(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            throw new RuntimeException("Could not delete file from Cloudinary", e);
        }
    }

}