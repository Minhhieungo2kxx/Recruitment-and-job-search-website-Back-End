package com.webjob.application.Repository;

import com.webjob.application.Model.Entity.TemporaryUpload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TemporaryUploadRepository extends JpaRepository<TemporaryUpload,Long> {

    Optional<TemporaryUpload> findByPublicId(String publicId);
    List<TemporaryUpload> findByUsedFalseAndCreatedAtBefore(Instant cutoff);

}
