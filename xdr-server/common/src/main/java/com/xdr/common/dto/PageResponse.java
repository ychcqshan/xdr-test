package com.xdr.common.dto;

import lombok.Data;

import java.util.List;

/**
 * 分页响应
 */
@Data
public class PageResponse<T> {

    private List<T> records;
    private long total;
    private long page;
    private long size;

    public static <T> PageResponse<T> of(List<T> records, long total, long page, long size) {
        PageResponse<T> response = new PageResponse<>();
        response.setRecords(records);
        response.setTotal(total);
        response.setPage(page);
        response.setSize(size);
        return response;
    }
}
