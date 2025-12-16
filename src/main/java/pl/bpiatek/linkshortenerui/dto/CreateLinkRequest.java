package pl.bpiatek.linkshortenerui.dto;


public record CreateLinkRequest(
        String longUrl,
        String shortUrl,
        Boolean isActive,
        String title) {
}
