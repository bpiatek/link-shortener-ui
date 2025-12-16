package pl.bpiatek.linkshortenerui.exception;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpClientErrorException.Unauthorized.class)
    public String handleUnauthorized() {
        // If we end up here, it means Access Token expired AND Refresh Token failed.
        // Force user to login again.
        return "redirect:/login";
    }
}