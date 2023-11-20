/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.rs;

import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * HTTP response status code.
 * See <a href="https://tools.ietf.org/html/rfc2616#section-6.1.1">RFC 2616 6.1.1 Status Code and Reason Phrase</a>
 *
 * @since 0.4
 */
public enum RsStatus {
    /**
     * Status <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/100">Continue</a>.
     */
    CONTINUE("100"),
    /**
     * OK.
     */
    OK("200"),
    /**
     * Created.
     */
    CREATED("201"),
    /**
     * Accepted.
     */
    ACCEPTED("202"),
    /**
     * No Content.
     */
    NO_CONTENT("204"),
    /**
     * Moved Permanently.
     */
    MOVED_PERMANENTLY("301"),
    /**
     * Found.
     */
    FOUND("302"),
    /**
     * Not Modified.
     */
    NOT_MODIFIED("304"),
    /**
     * Temporary Redirect.
     */
    @SuppressWarnings("PMD.LongVariable")
    TEMPORARY_REDIRECT("307"),
    /**
     * Bad Request.
     */
    BAD_REQUEST("400"),
    /**
     * Unauthorized.
     */
    UNAUTHORIZED("401"),
    /**
     * Forbidden.
     */
    FORBIDDEN("403"),
    /**
     * Not Found.
     */
    NOT_FOUND("404"),
    /**
     * Method Not Allowed.
     */
    @SuppressWarnings("PMD.LongVariable")
    METHOD_NOT_ALLOWED("405"),
    /**
     * Request Time-out.
     */
    REQUEST_TIMEOUT("408"),
    /**
     * Conflict.
     */
    CONFLICT("409"),
    /**
     * Length Required.
     */
    LENGTH_REQUIRED("411"),
    /**
     * Payload Too Large.
     */
    PAYLOAD_TOO_LARGE("413"),
    /**
     * Requested Range Not Satisfiable.
     */
    BAD_RANGE("416"),
    /**
     * Status <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/417">
     * Expectation Failed</a>.
     */
    EXPECTATION_FAILED("417"),
    /**
     * Misdirected Request.
     */
    MISDIRECTED_REQUEST("421"),
    /**
     * Too Many Requests.
     */
    TOO_MANY_REQUESTS("429"),
    /**
     * Internal Server Error.
     */
    INTERNAL_ERROR("500"),
    /**
     * Not Implemented.
     */
    NOT_IMPLEMENTED("501"),
    /**
     * Service Unavailable.
     */
    UNAVAILABLE("503");

    /**
     * Code value.
     */
    private final String string;

    /**
     * Ctor.
     *
     * @param string Code value.
     */
    RsStatus(final String string) {
        this.string = string;
    }

    /**
     * Code as 3-digit string.
     *
     * @return Code as 3-digit string.
     */
    public String code() {
        return this.string;
    }

    /**
     * Checks whether the RsStatus is an informational group (1xx).
     * @return True if the RsStatus is 1xx, otherwise - false.
     * @since 0.16
     */
    public boolean information() {
        return this.firstSymbol('1');
    }

    /**
     * Checks whether the RsStatus is a successful group (2xx).
     * @return True if the RsStatus is 2xx, otherwise - false.
     * @since 0.16
     */
    public boolean success() {
        return this.firstSymbol('2');
    }

    /**
     * Checks whether the RsStatus is a redirection.
     * @return True if the RsStatus is 3xx, otherwise - false.
     * @since 0.16
     */
    public boolean redirection() {
        return this.firstSymbol('3');
    }

    /**
     * Checks whether the RsStatus is a client error.
     * @return True if the RsStatus is 4xx, otherwise - false.
     * @since 0.16
     */
    public boolean clientError() {
        return this.firstSymbol('4');
    }

    /**
     * Checks whether the RsStatus is a server error.
     * @return True if the RsStatus is 5xx, otherwise - false.
     * @since 0.16
     */
    public boolean serverError() {
        return this.firstSymbol('5');
    }

    /**
     * Checks whether the RsStatus is an error.
     * @return True if the RsStatus is an error, otherwise - false.
     * @since 0.16
     */
    public boolean error() {
        return this.clientError() || this.serverError();
    }

    /**
     * Checks whether the first character matches the symbol.
     * @param symbol Symbol to check
     * @return True if the first character matches the symbol, otherwise - false.
     * @since 0.16
     */
    private boolean firstSymbol(final char symbol) {
        return this.string.charAt(0) == symbol;
    }

    /**
     * Searches {@link RsStatus} instance by response code.
     * @since 0.11
     */
    public static class ByCode {

        /**
         * Status code.
         */
        private final String code;

        /**
         * Ctor.
         * @param code Code
         */
        public ByCode(@Nullable final String code) {
            this.code = code;
        }

        /**
         * Ctor.
         * @param code Code
         */
        public ByCode(final int code) {
            this(String.valueOf(code));
        }

        /**
         * Searches RsStatus by code.
         * @return RsStatus instance if found
         * @throws IllegalArgumentException If RsStatus is not found
         */
        public RsStatus find() {
            return Stream.of(RsStatus.values())
                .filter(status -> status.code().equals(this.code))
                .findAny()
                .orElseThrow(
                    () -> new IllegalArgumentException(
                        String.format("Unknown status code: `%s`", this.code)
                    )
                );
        }
    }
}
