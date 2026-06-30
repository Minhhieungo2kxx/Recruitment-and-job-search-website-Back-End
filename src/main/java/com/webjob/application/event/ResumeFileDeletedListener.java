package com.webjob.application.event;

import com.webjob.application.dto.record.ResumeFileDeletedEvent;
import com.webjob.application.service.UploadFileServer.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResumeFileDeletedListener {
    private final FileService fileService;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    @Async("taskExecutor")
    public void handle(ResumeFileDeletedEvent event) {

        try {

            fileService.deleteFile(
                    event.getPublicId(),
                    event.getResourceType()
            );

        } catch (Exception ex) {

            log.error(
                    "Delete cloudinary file failed: {}",
                    event.getPublicId(),
                    ex
            );

        }

    }
}
