package pl.bpiatek.linkshortenerui.dto;

import java.time.Instant;
import java.util.List;

public record DashboardLinkResponse(
        String linkId,
        String shortUrl,
        String longUrl,
        String title,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        long totalClicks,
        List<MetricEntryResponse> clicksByCountry,
        List<MetricEntryResponse> clicksByDevice,
        List<MetricEntryResponse> clicksByOs
) {}