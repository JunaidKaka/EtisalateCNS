// com.example.api.response.PagedMeta.java (optional helper)
package com.etisalat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PagedMeta {
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
