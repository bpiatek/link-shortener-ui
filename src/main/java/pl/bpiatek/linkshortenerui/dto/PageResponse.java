package pl.bpiatek.linkshortenerui.dto;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public boolean isFirst() {
        return page == 0;
    }

    public boolean isLast() {
        return page >= totalPages - 1;
    }

    public boolean isEmpty() {
        return content == null || content.isEmpty();
    }
}
