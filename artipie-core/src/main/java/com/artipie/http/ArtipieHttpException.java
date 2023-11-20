/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.ArtipieException;
import com.artipie.http.rs.RsStatus;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

/**
 * Base HTTP exception for Artipie endpoints.
 * @since 1.0
 */
@SuppressWarnings("PMD.OnlyOneConstructorShouldDoInitialization")
public final class ArtipieHttpException extends ArtipieException {

    private static final long serialVersionUID = -16695752893817954L;

    /**
     * HTTP error codes reasons map.
     */
    private static final Map<String, String> MEANINGS = new ImmutableMap.Builder<String, String>()
        .put("400", "Bad request")
        .put("401", "Unauthorized")
        .put("402", "Payment Required")
        .put("403", "Forbidden")
        .put("404", "Not Found")
        .put("405", "Method Not Allowed")
        .put("406", "Not Acceptable")
        .put("407", "Proxy Authentication Required")
        .put("408", "Request Timeout")
        .put("409", "Conflict")
        .put("410", "Gone")
        .put("411", "Length Required")
        .put("412", "Precondition Failed")
        .put("413", "Payload Too Large")
        .put("414", "URI Too Long")
        .put("415", "Unsupported Media Type")
        .put("416", "Range Not Satisfiable")
        .put("417", "Expectation Failed")
        .put("418", "I'm a teapot")
        .put("421", "Misdirected Request")
        .put("422", "Unprocessable Entity (WebDAV)")
        .put("423", "Locked (WebDAV)")
        .put("424", "Failed Dependency (WebDAV)")
        .put("425", "Too Early")
        .put("426", "Upgrade Required")
        .put("428", "Precondition Required")
        .put("429", "Too Many Requests")
        .put("431", "Request Header Fields Too Large")
        .put("451", "Unavailable For Legal Reasons")
        .put("500", "Internal Server Error")
        .put("501", "Not Implemented")
        .build();

    /**
     * HTTP status code for error.
     */
    private final RsStatus code;

    /**
     * New HTTP error exception.
     * @param status HTTP status code
     */
    public ArtipieHttpException(final RsStatus status) {
        this(status, ArtipieHttpException.meaning(status));
    }

    /**
     * New HTTP error exception.
     * @param status HTTP status code
     * @param cause Of the error
     */
    public ArtipieHttpException(final RsStatus status, final Throwable cause) {
        this(status, ArtipieHttpException.meaning(status), cause);
    }

    /**
     * New HTTP error exception with custom message.
     * @param status HTTP status code
     * @param message HTTP status meaning
     */
    public ArtipieHttpException(final RsStatus status, final String message) {
        super(message);
        this.code = status;
    }

    /**
     * New HTTP error exception with custom message and cause error.
     * @param status HTTP status code
     * @param message HTTP status meaning
     * @param cause Of the error
     */
    public ArtipieHttpException(final RsStatus status, final String message,
        final Throwable cause) {
        super(message, cause);
        this.code = status;
    }

    /**
     * Status code.
     * @return RsStatus
     */
    public RsStatus status() {
        return this.code;
    }

    /**
     * The meaning of error code.
     * @param status HTTP status code for error
     * @return Meaning string for this code
     */
    private static String meaning(final RsStatus status) {
        return ArtipieHttpException.MEANINGS.getOrDefault(status.code(), "Unknown");
    }
}
