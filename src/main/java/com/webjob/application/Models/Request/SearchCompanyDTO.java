package com.webjob.application.Models.Request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SearchCompanyDTO {

    private String name;

    private String description;


  private String address;


    private String logo;

    private Instant createdAt;

    private String page;  // Default value
    private int limit = 5; // Default value
    private String sortBy; // Name or other fields
    private String sortOrder = "ASC"; // ASC or DESC

}
