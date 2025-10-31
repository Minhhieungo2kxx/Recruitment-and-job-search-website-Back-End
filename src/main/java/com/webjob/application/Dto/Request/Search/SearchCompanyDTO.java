package com.webjob.application.Dto.Request.Search;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

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
    private int limit = 8; // Default value
    private String sortBy; // Name or other fields
    private String sortOrder = "ASC"; // ASC or DESC

}
