package com.campusos.common;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Standard pagination envelope for every list endpoint. */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {
    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    public static <T> PageResponse<T> ofContent(List<T> content, long totalElements, int page, int size) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 1;
        return new PageResponse<>(content, page, size, totalElements, totalPages, page + 1 >= totalPages);
    }
}
