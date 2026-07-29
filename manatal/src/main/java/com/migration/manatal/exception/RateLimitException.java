package com.migration.manatal.exception;

public class RateLimitException extends RuntimeException{

        public RateLimitException(long retryAfterSeconds) {
            super("Rate limit exceeded. Please retry after " + retryAfterSeconds + " seconds.");
        }
}
