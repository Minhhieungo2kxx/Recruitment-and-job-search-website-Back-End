package com.webjob.application.models.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "temporary_uploads",
        indexes = {
                @Index(name = "idx_temp_used_created", columnList = "used, createdAt")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class TemporaryUpload {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String publicId;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private String resourceType;

    /**
     * Người upload
     */
    @ManyToOne
    @JoinColumn(name="user_id")
    private User user;

    /**
     * Thời điểm upload
     */
    @Column(nullable = false)
    @CreatedDate
    private Instant createdAt;


    @Column(nullable = false)
    private boolean used;
}
