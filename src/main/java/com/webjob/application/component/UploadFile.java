package com.webjob.application.component;

import com.webjob.application.enums.FileType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@RequiredArgsConstructor
@Component
public class UploadFile {
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES =
            Set.of(
                    "image/jpeg",
                    "image/png"
            );

    private static final String PDF_EXTENSION = "pdf";
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final UploadProperties uploadProperties;


    public FileType validateUploadFile(MultipartFile file) throws IOException {

        // Kiểm tra file null / empty
        validateCommonFile(file);

        //Lấy extension
        String extension = getFileExtension(file);

        // Lấy MIME type:image/jpeg, application/pdf, text/plain
        String contentType = file.getContentType();

        if (PDF_EXTENSION.equals(extension)) {

            validatePdf(file, contentType);

            return FileType.PDF;
        }

        if (ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {

            validateImage(file, contentType, extension);

            return FileType.IMAGE;
        }
        throw new IllegalStateException("Định dạng file không được hỗ trợ: ." + extension);
    }

    private void validatePdf(MultipartFile file, String contentType) throws IOException {

        // Check MIME type
        if (!PDF_CONTENT_TYPE.equalsIgnoreCase(contentType)) {
            throw new IllegalStateException("File PDF có Content-Type không hợp lệ.");
        }

        // Check magic bytes
        if (!isPdf(file)) {
            throw new IllegalStateException("Nội dung file không phải PDF hợp lệ.");
        }
    }
    private void validateImage(
            MultipartFile file,
            String contentType,
            String extension
    ) throws IOException {

        // Check MIME type
        if (!ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalStateException("File ảnh có Content-Type không hợp lệ.");
        }

        // JPG / JPEG
        if (extension.equals("jpg") || extension.equals("jpeg")) {
            if (!isJpeg(file)) {
                throw new IllegalStateException("Nội dung file không phải JPEG hợp lệ.");
            }

            return;
        }

        // PNG
        if (extension.equals("png")) {
            if (!isPng(file)) {
                throw new IllegalStateException("Nội dung file không phải PNG hợp lệ.");
            }
        }
    }

    private void validateCommonFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalStateException("File is empty, please enter file");
        }

        // Check size
        if (file.getSize() > uploadProperties.getMaxFileSize().toBytes()) {

            throw new IllegalStateException("File vượt quá kích thước tối đa cho phép.");
        }

        // Check filename
        String originalFileName = file.getOriginalFilename();

        if (originalFileName == null || !originalFileName.contains(".")) {

            throw new IllegalStateException("File không có phần mở rộng hợp lệ.");
        }
    }

    private String getFileExtension(MultipartFile file) {

        String originalFileName = file.getOriginalFilename();

        return originalFileName
                .substring(originalFileName.lastIndexOf('.') + 1)
                .toLowerCase();
    }

    private boolean isPdf(MultipartFile file) throws IOException {

        byte[] bytes = file.getBytes();

        return bytes.length >= 4
                && bytes[0] == 0x25
                && bytes[1] == 0x50
                && bytes[2] == 0x44
                && bytes[3] == 0x46;
    }

    private boolean isJpeg(MultipartFile file) throws IOException {
        byte[] bytes = file.getBytes();
        return bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(MultipartFile file) throws IOException {

        byte[] bytes = file.getBytes();

        return bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && (bytes[1] & 0xFF) == 0x50
                && (bytes[2] & 0xFF) == 0x4E
                && (bytes[3] & 0xFF) == 0x47
                && (bytes[4] & 0xFF) == 0x0D
                && (bytes[5] & 0xFF) == 0x0A
                && (bytes[6] & 0xFF) == 0x1A
                && (bytes[7] & 0xFF) == 0x0A;
    }


}
