package com.webjob.application.Models.Response;

import com.webjob.application.Models.Enums.ResumeStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeResponse {

    private Long id;


    private String email;


    private String url;


    private ResumeStatus status;


    private Instant createdAt;


    private Instant updatedAt;


    private String createdBy;

    private String updatedBy;
    private User user;
    private Job job;
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class User{

        private long id;
        private String fullName;

    }
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Job{

        private long id;
        private String name;

    }

}
