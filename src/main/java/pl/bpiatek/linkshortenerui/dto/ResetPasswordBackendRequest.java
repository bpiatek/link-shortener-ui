package pl.bpiatek.linkshortenerui.dto;

public record ResetPasswordBackendRequest(String token, String newPassword) {
}
