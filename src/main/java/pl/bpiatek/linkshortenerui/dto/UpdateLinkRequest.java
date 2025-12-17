package pl.bpiatek.linkshortenerui.dto;

public record UpdateLinkRequest(String longUrl,
                         boolean isActive,
                         String title) {
}
