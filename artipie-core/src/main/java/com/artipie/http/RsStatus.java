/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http;


import org.apache.hc.core5.http.HttpStatus;

import java.util.stream.Stream;

/**
 * HTTP response status code.
 * See <a href="https://tools.ietf.org/html/rfc2616#section-6.1.1">RFC 2616 6.1.1 Status Code and Reason Phrase</a>
 */
public enum RsStatus {
    /**
     * Status <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/100">Continue</a>.
     */
    CONTINUE(HttpStatus.SC_CONTINUE),
    /**
     * OK.
     */
    OK(HttpStatus.SC_OK),
    /**
     * Created.
     */
    CREATED(HttpStatus.SC_CREATED),
    /**
     * Accepted.
     */
    ACCEPTED(HttpStatus.SC_ACCEPTED),
    /**
     * No Content.
     */
    NO_CONTENT(HttpStatus.SC_NO_CONTENT),
    /**
     * Moved Permanently.
     */
    MOVED_PERMANENTLY(HttpStatus.SC_MOVED_PERMANENTLY),
    /**
     * Found.
     */
    MOVED_TEMPORARILY(HttpStatus.SC_MOVED_TEMPORARILY),
    /**
     * Not Modified.
     */
    NOT_MODIFIED(HttpStatus.SC_NOT_MODIFIED),
    /**
     * Temporary Redirect.
     */
    TEMPORARY_REDIRECT(HttpStatus.SC_TEMPORARY_REDIRECT),
    /**
     * Bad Request.
     */
    BAD_REQUEST(HttpStatus.SC_BAD_REQUEST),
    /**
     * Unauthorized.
     */
    UNAUTHORIZED(HttpStatus.SC_UNAUTHORIZED),
    /**
     * Forbidden.
     */
    FORBIDDEN(HttpStatus.SC_FORBIDDEN),
    /**
     * Not Found.
     */
    NOT_FOUND(HttpStatus.SC_NOT_FOUND),
    /**
     * Method Not Allowed.
     */
    @SuppressWarnings("PMD.LongVariable")
    METHOD_NOT_ALLOWED(HttpStatus.SC_METHOD_NOT_ALLOWED),
    /**
     * Request Time-out.
     */
    REQUEST_TIMEOUT(HttpStatus.SC_REQUEST_TIMEOUT),
    /**
     * Conflict.
     */
    CONFLICT(HttpStatus.SC_CONFLICT),
    /**
     * Length Required.
     */
    LENGTH_REQUIRED(HttpStatus.SC_LENGTH_REQUIRED),
    /**
     * Payload Too Large.
     */
    REQUEST_TOO_LONG(HttpStatus.SC_REQUEST_TOO_LONG),
    /**
     * Requested Range Not Satisfiable.
     */
    REQUESTED_RANGE_NOT_SATISFIABLE(HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE),
    /**
     * Status <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/417">
     * Expectation Failed</a>.
     */
    EXPECTATION_FAILED(HttpStatus.SC_EXPECTATION_FAILED),
    /**
     * Too Many Requests.
     */
    TOO_MANY_REQUESTS(HttpStatus.SC_TOO_MANY_REQUESTS),
    /**
     * Internal Server Error.
     */
    INTERNAL_ERROR(HttpStatus.SC_INTERNAL_SERVER_ERROR),
    /**
     * Not Implemented.
     */
    NOT_IMPLEMENTED(HttpStatus.SC_NOT_IMPLEMENTED),
    /**
     * Service Unavailable.
     */
    SERVICE_UNAVAILABLE(HttpStatus.SC_SERVICE_UNAVAILABLE);

    /**
     * Code value.
     */
    private final int code;

    /**
     * @param code Code value.
     */
    RsStatus(int code) {
        this.code = code;
    }

    public int code(){
        return code;
    }

    /**
     * Code as 3-digit string.
     *
     * @return Code as 3-digit string.
     */
    public String asString() {
        return String.valueOf(this.code);
    }

    /**
     * Checks whether the RsStatus is an informational group (1xx).
     * @return True if the RsStatus is 1xx, otherwise - false.
     */
    public boolean information() {
        return this.firstSymbol('1');
    }

    /**
     * Checks whether the RsStatus is a successful group (2xx).
     * @return True if the RsStatus is 2xx, otherwise - false.
     */
    public boolean success() {
        return this.firstSymbol('2');
    }

    /**
     * Checks whether the RsStatus is a redirection.
     * @return True if the RsStatus is 3xx, otherwise - false.
     */
    public boolean redirection() {
        return this.firstSymbol('3');
    }

    /**
     * Checks whether the RsStatus is a client error.
     * @return True if the RsStatus is 4xx, otherwise - false.
     */
    public boolean clientError() {
        return this.firstSymbol('4');
    }

    /**
     * Checks whether the RsStatus is a server error.
     * @return True if the RsStatus is 5xx, otherwise - false.
     */
    public boolean serverError() {
        return this.firstSymbol('5');
    }

    /**
     * Checks whether the RsStatus is an error.
     * @return True if the RsStatus is an error, otherwise - false.
     */
    public boolean error() {
        return this.clientError() || this.serverError();
    }

    /**
     * Checks whether the first character matches the symbol.
     * @param symbol Symbol to check
     * @return True if the first character matches the symbol, otherwise - false.
     */
    private boolean firstSymbol(final char symbol) {
        return asString().charAt(0) == symbol;
    }

    public static RsStatus byCode(int code) {
        return Stream.of(RsStatus.values())
            .filter(s -> s.code == code).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported status code: " + code));
    }
}
