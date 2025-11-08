package com.webjob.application.Config.UploadfileServer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

@Component
public class UploadFile {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png", "doc", "docx");


    private final UploadProperties uploadProperties;

    public UploadFile(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }


    public String getnameFile(MultipartFile file, String nameFolder) throws IOException {
        String target = "";
        vadidateUploadFile(file, nameFolder);
        String originalFileName = file.getOriginalFilename();
        // Đường dẫn upload lấy từ cấu hình
        String uploadDir = uploadProperties.getBaseDir() + "/" + nameFolder;
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        String uniqueFileName =System.currentTimeMillis()+"-"+originalFileName;
        Path filePath = uploadPath.resolve(uniqueFileName);
        file.transferTo(filePath.toFile());
        target = uniqueFileName;

        return target;
    }

    public void vadidateUploadFile(MultipartFile file, String nameFolder) throws IOException, IllegalStateException {
        if (file != null && !file.isEmpty()) {
            // Kiểm tra kích thước file
            if (file.getSize() >uploadProperties.getMaxFileSize().toBytes()) {
                throw new IllegalStateException("File vượt quá kích thước tối đa cho phép.");
            }
            String originalFileName = file.getOriginalFilename();

            if (originalFileName == null || !originalFileName.contains(".")) {
                throw new IllegalStateException("File không có phần mở rộng hợp lệ.");
            }

            String extension = originalFileName.substring(originalFileName.lastIndexOf('.') + 1).toLowerCase();

            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new IllegalStateException("Định dạng file không được hỗ trợ: ." + extension);
            }

        } else {
            throw new IllegalStateException("File current is empty,Please enter file");
        }

    }
}
