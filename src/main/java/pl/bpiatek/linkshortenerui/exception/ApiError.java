package pl.bpiatek.linkshortenerui.exception;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        String type,
        String title,
        int status,
        String detail,
        String instance,
        @JsonAlias("errors")
        List<ValidationError> validationErrors
) {}
