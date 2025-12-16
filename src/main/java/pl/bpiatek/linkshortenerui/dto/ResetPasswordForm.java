package pl.bpiatek.linkshortenerui.dto;

public record ResetPasswordForm(String token, String password, String confirmPassword) {}
