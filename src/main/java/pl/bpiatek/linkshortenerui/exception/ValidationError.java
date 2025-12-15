package pl.bpiatek.linkshortenerui.exception;

public record ValidationError(
        String field,
        Object rejectedValue,
        String message
) {}