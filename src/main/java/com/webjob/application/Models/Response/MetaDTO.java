package com.webjob.application.Models.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MetaDTO {
    private int current;
    private int pageSize;
    private int pages;
    private long total;
}
