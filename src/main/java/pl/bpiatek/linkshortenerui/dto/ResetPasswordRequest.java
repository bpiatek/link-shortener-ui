package pl.bpiatek.linkshortenerui.dto;

public record ResetPasswordRequest(String token, String password, String confirmPassword) {}
