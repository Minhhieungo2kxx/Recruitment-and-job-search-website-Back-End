package com.webjob.application.Util.Cron;

import com.webjob.application.Model.Entity.TemporaryUpload;
import com.webjob.application.Repository.TemporaryUploadRepository;
import com.webjob.application.Service.UploadFileServer.FileService;
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

//        Instant cutoff = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant cutoff = Instant.now().minus(1, ChronoUnit.MINUTES);
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
