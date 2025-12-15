package pl.bpiatek.linkshortenerui.dto;

import java.time.Instant;

public record LinkDto(
        Long id,
        String linkId,
        String userId,
        String shortUrl,
        String longUrl,
        String title,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt,
        int totalClicks
) {}