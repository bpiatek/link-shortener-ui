package pl.bpiatek.linkshortenerui.exception;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;

import java.util.logging.Logger;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@ControllerAdvice
public class GlobalExceptionHandler {

    Logger log = Logger.getLogger(GlobalExceptionHandler.class.getName());

    @ExceptionHandler(HttpClientErrorException.class)
    public String handleHttpClientError(HttpClientErrorException ex) {

        if (ex.getStatusCode() == UNAUTHORIZED) {
            log.info("GlobalExceptionHandler caught 401 - Redirecting to login");
            return "redirect:/login";
        }

        throw ex;
    }
}