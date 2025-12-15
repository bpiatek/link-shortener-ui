package pl.bpiatek.linkshortenerui.exception;

import java.util.List;

public record ApiError(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        List<ValidationError> validationErrors
) {}
