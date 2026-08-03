package com.migration.manatal.exception;

import org.springframework.http.HttpStatus;

public class NonRetryableApiException extends ApiException {

    public NonRetryableApiException(HttpStatus status, String responseBody) {
        super(status, responseBody);
    }

}
