package com.shirtshop.common;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    @Value("${app.cloudinary.default-folder:shirtshop}")
    private String defaultFolder;

    public Map<String, Object> uploadImage(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("File is empty");

        String targetFolder = (folder != null && !folder.isBlank()) ? folder : defaultFolder;
        @SuppressWarnings("unchecked")
        Map<String, Object> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", targetFolder,
                        "resource_type", "image",
                        "overwrite", true
                )
        );

        return Map.of(
                "url", result.get("secure_url"),
                "publicId", result.get("public_id"),
                "width", result.get("width"),
                "height", result.get("height"),
                "format", result.get("format")
        );
    }

    public List<Map<String, Object>> uploadImages(List<MultipartFile> files, String folder) throws IOException {
        if (files == null || files.isEmpty()) throw new IllegalArgumentException("No files");
        List<Map<String, Object>> out = new ArrayList<>();
        for (MultipartFile f : files) out.add(uploadImage(f, folder));
        return out;
    }

    public Map<String, Object> deleteByPublicId(String publicId) throws IOException {
        if (publicId == null || publicId.isBlank()) throw new IllegalArgumentException("publicId is required");
        @SuppressWarnings("unchecked")
        Map<String, Object> res = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        return res;
    }
}
