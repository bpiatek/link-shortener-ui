package pl.bpiatek.linkshortenerui.dto;

public record UpdateLinkRequest(String longUrl,
                         Boolean isActive,
                         String title) {
}
