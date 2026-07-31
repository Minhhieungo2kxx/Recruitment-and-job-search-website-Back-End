package com.webjob.application.service.UploadFileServer;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.webjob.application.config.UploadfileServer.UploadFile;
import com.webjob.application.config.UploadfileServer.UploadProperties;
import com.webjob.application.dto.Response.ApiResponse;
import com.webjob.application.dto.Response.FileDownloadResponseDto;
import com.webjob.application.dto.Response.UploadFileResponse;
import com.webjob.application.exception.Customs.BadRequestException;
import com.webjob.application.models.Entity.TemporaryUpload;
import com.webjob.application.models.Entity.User;
import com.webjob.application.repository.TemporaryUploadRepository;
import com.webjob.application.utils.common.Base64Util;
import com.webjob.application.utils.common.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FileService {
    private final UploadProperties uploadProperties;

    private final Cloudinary cloudinary;

    private final UploadFile uploadFile;

    private final TemporaryUploadRepository temporaryUploadRepository;

    private final SecurityUtils securityUtils;



//public  Resource  handledownloadFile(String folder, String filename) {
//
//    Path baseDir = Paths.get(uploadProperties.getBaseDir())
//            .toAbsolutePath()
//            .normalize();
//
//    Path filePath = baseDir.resolve(folder)
//            .resolve(filename)
//            .normalize();
//
//    // Chống path traversal
//    if (!filePath.startsWith(baseDir) || !Files.exists(filePath)) {
//        throw new BadRequestException("File Path not found");
//    }
//
//    Resource resource = null;
//    try {
//        resource = new UrlResource(filePath.toUri());
//    } catch (MalformedURLException e) {
//        throw new RuntimeException(e);
//    }
//
//    String contentType = null;
//    try {
//        contentType = Files.probeContentType(filePath);
//    } catch (IOException e) {
//        throw new RuntimeException(e);
//    }
//    if (contentType == null) {
//        contentType = "application/octet-stream";
//    }
//
//    String encodedFilename = URLEncoder.encode(
//            resource.getFilename(),
//            StandardCharsets.UTF_8)
//            .replace("+", "%20");
//    return resource;
//}
//public FileDownloadResponseDto downloadFile(String folder, String filename)  {
//
//    Path baseDir = Paths.get(uploadProperties.getBaseDir())
//            .toAbsolutePath()
//            .normalize();
//
//    Path filePath = baseDir.resolve(folder)
//            .resolve(filename)
//            .normalize();
//
//    if (!filePath.startsWith(baseDir) || !Files.exists(filePath)) {
//        try {
//            throw new FileNotFoundException("File not found");
//        } catch (FileNotFoundException e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    Resource resource = null;
//    try {
//        resource = new UrlResource(filePath.toUri());
//    } catch (MalformedURLException e) {
//        throw new RuntimeException(e);
//    }
//
//    String contentType = null;
//    try {
//        contentType = Files.probeContentType(filePath);
//    } catch (IOException e) {
//        throw new RuntimeException(e);
//    }
//    if (contentType == null) {
//        contentType = "application/octet-stream";
//    }
//
//    String encodedFilename = URLEncoder.encode(
//            resource.getFilename(),
//            StandardCharsets.UTF_8
//    ).replace("+", "%20");
//
//    try {
//        return FileDownloadResponseDto.builder()
//                .resource(resource)
//                .contentType(contentType)
//                .encodedFilename(encodedFilename)
//                .fileSize(Files.size(filePath))
//                .build();
//    } catch (IOException e) {
//        throw new RuntimeException(e);
//    }
//}

public FileDownloadResponseDto proxyDownloadCloudinary(String encodedUrl)  {

    String decodedUrl = Base64Util.decode(encodedUrl);

    if (!decodedUrl.startsWith("https://res.cloudinary.com/")) {
        throw new BadRequestException("Invalid Cloudinary URL");
    }

    URL url = null;
    try {
        url = new URL(decodedUrl);
    } catch (MalformedURLException e) {
        throw new RuntimeException(e);
    }
    HttpURLConnection connection = null;
    try {
        connection = (HttpURLConnection) url.openConnection();
    } catch (IOException e) {
        throw new RuntimeException(e);
    }
    try {
        connection.setRequestMethod("GET");
    } catch (ProtocolException e) {
        throw new RuntimeException(e);
    }

    int statusCode = 0;
    try {
        statusCode = connection.getResponseCode();
    } catch (IOException e) {
        throw new RuntimeException(e);
    }

    if (statusCode != HttpURLConnection.HTTP_OK) {
        try {
            throw new IOException("Cloudinary response: " + statusCode);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    String contentType = connection.getContentType();
    if (contentType == null) {
        contentType = "application/octet-stream";
    }

    String fileName = Paths.get(url.getPath())
            .getFileName()
            .toString();

    InputStreamResource resource =
            null;
    try {
        resource = new InputStreamResource(connection.getInputStream());
    } catch (IOException e) {
        throw new RuntimeException(e);
    }

    return FileDownloadResponseDto.builder()
            .resource(resource)
            .contentType(contentType)
            .encodedFileName(URLEncoder.encode(fileName, StandardCharsets.UTF_8)
            )
            .statusCode(statusCode)
            .build();
}

    public Map<String, String> uploadFile(MultipartFile file, String folderName) throws IOException {
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
        handleTemporaryUpload(publicId, secureUrl, resourceType);

        return Map.of(
                "url", secureUrl,
                "publicId", publicId,
                "resourceType", resourceType //  TRẢ VỀ
        );
    }

    public void handleTemporaryUpload(String publicId, String secureUrl, String resourceType) {
        User user=securityUtils.getCurrentUser();
        TemporaryUpload temporaryUpload=TemporaryUpload.builder()
                .publicId(publicId)
                .url(secureUrl)
                .resourceType(resourceType)
                .user(user)
                .used(false)
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

public UploadFileResponse  uploadFileCloudinary(MultipartFile file,String folder)  {
    Map<String,String> uploadedFileName = null;
    try {
        uploadedFileName = uploadFile(file, folder);
    } catch (IOException e) {
        throw new RuntimeException(e);
    }
    UploadFileResponse uploadFileResponse =UploadFileResponse.builder()
            .fileName(uploadedFileName.get("url"))
            .public_id(uploadedFileName.get("publicId"))
            .resourceType(uploadedFileName.get("resourceType"))
            .uploadedAt(Instant.now())
            .fileSize(file.getSize())
            .contentType(file.getContentType())
            .folder(folder).build();
    return uploadFileResponse;
}

    public UploadFileResponse uploadFileServer (MultipartFile file, String folder){
        String uploadedFileName = null;
        try {
            uploadedFileName = uploadFile.getnameFile(file, folder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        UploadFileResponse uploadFileResponse =UploadFileResponse.builder()
                .fileName(uploadedFileName).uploadedAt(Instant.now())
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .folder(folder).build();
        return uploadFileResponse;
    }

}
