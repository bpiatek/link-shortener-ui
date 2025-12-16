package pl.bpiatek.linkshortenerui.exception;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;

import java.util.logging.Logger;

@ControllerAdvice
public class GlobalExceptionHandler {

    Logger log = Logger.getLogger(GlobalExceptionHandler.class.getName());

    @ExceptionHandler(HttpClientErrorException.Unauthorized.class)
    public String handleUnauthorized() {
        log.info("GlobalExceptionHandler caught 401 - Redirecting to login");
        // If we end up here, it means Access Token expired AND Refresh Token failed.
        // Force user to login again.
        return "redirect:/login";
    }
}