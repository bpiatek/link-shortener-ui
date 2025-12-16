package pl.bpiatek.linkshortenerui.dto;

public record RegisterRequest(String email, String password, String confirmPassword) {}

