package com.webjob.application.Service.UploadFileServer;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.webjob.application.Config.UploadfileServer.UploadFile;
import com.webjob.application.Config.UploadfileServer.UploadProperties;
import com.webjob.application.Model.Entity.TemporaryUpload;
import com.webjob.application.Repository.TemporaryUploadRepository;
import com.webjob.application.Util.Base64Util;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FileService {
    private final UploadProperties uploadProperties;

    private final Cloudinary cloudinary;

    private final UploadFile uploadFile;

    private final TemporaryUploadRepository temporaryUploadRepository;


    public ResponseEntity<?> handledownloadFile(String folder, String filename) {
        try {
            Path baseDir = Paths.get(uploadProperties.getBaseDir())
                    .toAbsolutePath()
                    .normalize();

            Path filePath = baseDir.resolve(folder)
                    .resolve(filename)
                    .normalize();

            // Chống path traversal
            if (!filePath.startsWith(baseDir) || !Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            String encodedFilename = URLEncoder.encode(
                    resource.getFilename(),
                    StandardCharsets.UTF_8
            ).replace("+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename*=UTF-8''" + encodedFilename
                    )
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public ResponseEntity<?> proxyDownloadCloudinary(String encodedUrl) {
        try {
            String decodedUrl = Base64Util.decode(encodedUrl);

            // Chỉ cho phép proxy đến Cloudinary
            if (!decodedUrl.startsWith("https://res.cloudinary.com/")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            URL url = new URL(decodedUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int statusCode = connection.getResponseCode();
            if (statusCode != HttpURLConnection.HTTP_OK) {
                return ResponseEntity.status(statusCode).build();
            }

            String contentType = connection.getContentType();
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            //  Lấy tên file từ URL (phần sau cùng)
            String fileName = Paths.get(url.getPath()).getFileName().toString();

            // Stream dữ liệu trực tiếp (không load toàn bộ vào RAM)
            InputStream inputStream = connection.getInputStream();
            InputStreamResource resource = new InputStreamResource(inputStream);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public Map<String, String> uploadFile(MultipartFile file, String folderName,Authentication authentication) throws IOException {
        uploadFile.vadidateUploadFile(file, folderName);
        String originalName = file.getOriginalFilename();
        String baseName = originalName.substring(0, originalName.lastIndexOf("."));
        String uniqueName = System.currentTimeMillis() + "-" + baseName;

        Map<?, ?> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", folderName,
                        "public_id", uniqueName,
                        "resource_type", "auto",
                        "access_mode", "public"

                )
        );

        String secureUrl = result.get("secure_url").toString();
        String publicId = result.get("public_id").toString();
        String resourceType = result.get("resource_type").toString();
        handleTemporaryUpload(publicId, secureUrl, resourceType,authentication);

        return Map.of(
                "url", secureUrl,
                "publicId", publicId,
                "resourceType", resourceType //  TRẢ VỀ
        );
    }

    public void handleTemporaryUpload(String publicId, String secureUrl, String resourceType,Authentication authentication) {
        TemporaryUpload temporaryUpload=TemporaryUpload.builder()
                .publicId(publicId)
                .url(secureUrl)
                .resourceType(resourceType)
                .used(false)
                .userId(Long.valueOf(authentication.getName()))
                .build();
        temporaryUploadRepository.save(temporaryUpload);

    }


    public void deleteFile(String publicId, String resourceType) throws IOException {

        Map<?, ?> result = cloudinary.uploader().destroy(
                publicId,
                ObjectUtils.asMap("resource_type", resourceType)
        );

        String resultStatus = result.get("result").toString();

        if (!"ok".equals(resultStatus)) {
            throw new IllegalStateException("Không thể xóa file trên Cloudinary: " + resultStatus);
        }
    }
}
