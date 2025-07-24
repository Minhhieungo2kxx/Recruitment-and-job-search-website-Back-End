package com.webjob.application.Models.Response;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private long id;

    private String email;

    private String fullName;

    private Integer age;

    private String gender; // MALE/FEMALE

    private String address;


    private Instant createdAt;

    private Instant updatedAt;

    private String createdBy;

    private String updatedBy;

    private Company company;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Company{
        private Long id;
        private String name;


    }

}
