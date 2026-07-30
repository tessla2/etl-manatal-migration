package com.migration.manatal.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String responseBody;

    public ApiException(HttpStatus status, String responseBody) {
        super(responseBody);
        this.status = status;
        this.responseBody = responseBody;
    }

    public ApiException(HttpStatus status, String responseBody, Throwable cause) {
        super(responseBody, cause);
        this.status = status;
        this.responseBody = responseBody;
    }

    public static ApiException notFound(String detail) {
        return new ApiException(HttpStatus.NOT_FOUND, detail);
    }

    public static ApiException badGateway(String detail) {
        return new ApiException(HttpStatus.BAD_GATEWAY, detail);
    }

    public static ApiException badGateway(String detail, Throwable cause) {
        return new ApiException(HttpStatus.BAD_GATEWAY, detail, cause);
    }

}
