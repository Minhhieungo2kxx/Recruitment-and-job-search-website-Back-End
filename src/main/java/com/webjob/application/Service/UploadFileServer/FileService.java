package com.webjob.application.Service.UploadFileServer;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.webjob.application.Config.UploadfileServer.UploadFile;
import com.webjob.application.Config.UploadfileServer.UploadProperties;
import com.webjob.application.Util.Base64Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Service
public class FileService {
    private final UploadProperties uploadProperties;

    private final Cloudinary cloudinary;

    private final UploadFile uploadFile;

    public FileService(UploadProperties uploadProperties, Cloudinary cloudinary, UploadFile uploadFile) {
        this.uploadProperties = uploadProperties;
        this.cloudinary = cloudinary;
        this.uploadFile = uploadFile;
    }
    public ResponseEntity<?> handledownloadFile(String folder, String filename) {
        try {
            Path baseDir = Paths.get(uploadProperties.getBaseDir()).toAbsolutePath().normalize();
            Path filePath = baseDir.resolve(folder).resolve(filename).normalize();

            if (!filePath.startsWith(baseDir) || !Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                if (filename.endsWith(".pdf")) contentType = "application/pdf";
                else if (filename.matches(".*\\.(png|jpg|jpeg|gif)$")) contentType = "image/*";
                else contentType = "application/octet-stream";
            }

            String disposition = (contentType.startsWith("image/") || contentType.equals("application/pdf"))
                    ? "inline"
                    : "attachment";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
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

    public String uploadFile(MultipartFile file, String folderName) throws IOException {
        uploadFile.vadidateUploadFile(file, folderName);
        String originalName = file.getOriginalFilename();
        String baseName = originalName.substring(0, originalName.lastIndexOf("."));
        String uniqueName = System.currentTimeMillis() + "-" + baseName;
        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "folder", folderName,
                "public_id", uniqueName,
                "access_mode", "public" // Chỉ định quyền truy cập công khai
        ));

        return result.get("secure_url").toString(); // Trả về link trực tiếp tới file trên Cloudinary
    }

}
