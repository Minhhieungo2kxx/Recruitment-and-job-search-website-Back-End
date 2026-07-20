package com.webjob.application.dto.Response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.webjob.application.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

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

    private LocalDate dateOfBirth;

    private UserStatus status;

    private boolean deleted;


    private Instant createdAt;



    private String createdBy;




    private Company company;
    private Role role;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Company{
        private Long id;
        private String name;


    }
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Role{
        private Long id;
        private String code;

    }

}
