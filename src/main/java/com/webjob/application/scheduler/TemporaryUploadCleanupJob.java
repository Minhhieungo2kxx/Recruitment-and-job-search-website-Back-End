package com.webjob.application.scheduler;

import com.webjob.application.models.Entity.TemporaryUpload;
import com.webjob.application.repository.TemporaryUploadRepository;
import com.webjob.application.service.UploadFileServer.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TemporaryUploadCleanupJob {
    private final TemporaryUploadRepository repository;
    private final FileService fileService;

    @Scheduled(cron = "0 0 3 * * ?") // 3h sáng mỗi ngày
    @Transactional
    public void cleanup() {

        Instant cutoff = Instant.now().minus(1, ChronoUnit.DAYS);
        List<TemporaryUpload> unused =
                repository.findByUsedFalseAndCreatedAtBefore(cutoff);

        for (TemporaryUpload file : unused) {

            try {
                fileService.deleteFile(
                        file.getPublicId(),
                        file.getResourceType()
                );

                repository.delete(file);

            } catch (Exception e) {
                log.error("Xóa file thất bại {}", file.getPublicId(), e);
            }
        }

        log.info("Đã dọn {} file rác", unused.size());
    }
}
