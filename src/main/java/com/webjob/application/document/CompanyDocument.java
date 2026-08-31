package com.webjob.application.document;

import co.elastic.clients.elasticsearch._types.mapping.FieldType;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyDocument {
    private Long id;

    private String name;

    private String description;

    private String address;

    private String logo;

    private String website;

    private String email;


    private String phone;

    private Integer employeeSize;


    private Integer foundedYear;

    private String status;

    private Boolean deleted;

    private String taxCode;

    private Long industryId;


    private String industryName;


    private Instant createdAt;

}
