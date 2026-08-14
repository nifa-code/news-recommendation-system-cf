package com.fanninews.recommender.Exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
public class ServiceException extends RuntimeException{
    private String errorCode;
    private String message;
    private HttpStatus statusCode;

    public ServiceException(String errorCode, String message) {
        this(errorCode, message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
