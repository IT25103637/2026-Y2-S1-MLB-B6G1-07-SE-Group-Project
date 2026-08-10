package com.safari.wildlife.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class VehicleUnavailableException extends RuntimeException {
    public VehicleUnavailableException(String message) {
        super(message);
    }
}
