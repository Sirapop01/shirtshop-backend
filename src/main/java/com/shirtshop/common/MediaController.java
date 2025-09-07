package com.shirtshop.common;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final CloudinaryService cloudinaryService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestPart("file") MultipartFile file,
                                    @RequestParam(value = "folder", required = false) String folder) throws Exception {
        Map<String, Object> info = cloudinaryService.uploadImage(file, folder);
        return ResponseEntity.ok(info);
    }

    @PostMapping(value = "/upload-multi", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadMulti(@RequestPart("files") List<MultipartFile> files,
                                         @RequestParam(value = "folder", required = false) String folder) throws Exception {
        var info = cloudinaryService.uploadImages(files, folder);
        return ResponseEntity.ok(info);
    }

    @DeleteMapping("/{publicId}")
    public ResponseEntity<?> delete(@PathVariable String publicId) throws Exception {
        var res = cloudinaryService.deleteByPublicId(publicId);
        return ResponseEntity.ok(res);
    }
}
