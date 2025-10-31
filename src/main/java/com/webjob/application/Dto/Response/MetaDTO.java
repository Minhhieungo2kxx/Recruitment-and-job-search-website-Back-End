package com.webjob.application.Dto.Response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MetaDTO {
    private int current;
    private int pageSize;
    private int pages;
    private long total;
}
