/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.asto.Content;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.headers.ContentType;
import com.artipie.http.headers.Header;
import com.artipie.http.rs.RsStatus;
import org.reactivestreams.Publisher;

import javax.json.JsonStructure;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class ResponseBuilder {

    public static ResponseBuilder from(RsStatus status) {
        return new ResponseBuilder(status);
    }

    public static ResponseBuilder ok() {
        return new ResponseBuilder(RsStatus.OK);
    }

    public static ResponseBuilder created() {
        return new ResponseBuilder(RsStatus.CREATED);
    }

    public static ResponseBuilder accepted() {
        return new ResponseBuilder(RsStatus.ACCEPTED);
    }

    public static ResponseBuilder temporaryRedirect() {
        return new ResponseBuilder(RsStatus.TEMPORARY_REDIRECT);
    }

    public static ResponseBuilder movedPermanently() {
        return new ResponseBuilder(RsStatus.MOVED_PERMANENTLY);
    }

    public static ResponseBuilder notFound() {
        return new ResponseBuilder(RsStatus.NOT_FOUND);
    }

    public static ResponseBuilder noContent() {
        return new ResponseBuilder(RsStatus.NO_CONTENT);
    }

    public static ResponseBuilder unavailable() {
        return new ResponseBuilder(RsStatus.SERVICE_UNAVAILABLE);
    }

    public static ResponseBuilder unauthorized() {
        return new ResponseBuilder(RsStatus.UNAUTHORIZED);
    }

    public static ResponseBuilder forbidden() {
        return new ResponseBuilder(RsStatus.FORBIDDEN);
    }

    public static ResponseBuilder methodNotAllowed() {
        return new ResponseBuilder(RsStatus.METHOD_NOT_ALLOWED);
    }

    public static ResponseBuilder badRequest() {
        return new ResponseBuilder(RsStatus.BAD_REQUEST);
    }

    public static ResponseBuilder badRequest(Throwable error) {
        return new ResponseBuilder(RsStatus.BAD_REQUEST)
            .body(errorBody(error));
    }

    public static ResponseBuilder payloadTooLarge() {
        return new ResponseBuilder(RsStatus.REQUEST_TOO_LONG);
    }

    public static ResponseBuilder internalError() {
        return new ResponseBuilder(RsStatus.INTERNAL_ERROR);
    }

    public static ResponseBuilder internalError(Throwable error) {
        return new ResponseBuilder(RsStatus.INTERNAL_ERROR)
            .body(errorBody(error));
    }

    private static byte[] errorBody(Throwable error) {
        StringBuilder res = new StringBuilder();
        res.append(error.getMessage()).append('\n');
        Throwable cause = error.getCause();
        if (cause != null) {
            res.append(cause.getMessage()).append('\n');
            if (cause.getSuppressed() != null) {
                for (final Throwable suppressed : cause.getSuppressed()) {
                    res.append(suppressed.getMessage()).append('\n');
                }
            }
        }
        return res.toString().getBytes();
    }

    private final RsStatus status;
    private Headers headers;
    private Content body;

    ResponseBuilder(RsStatus status) {
        this.status = status;
        this.headers = new Headers();
        this.body = Content.EMPTY;
    }

    public ResponseBuilder headers(Headers headers) {
        this.headers = headers.copy();
        return this;
    }

    public ResponseBuilder header(String name, String val) {
        this.headers.add(name, val);
        return this;
    }

    public ResponseBuilder header(Header header) {
        this.headers.add(header, false);
        return this;
    }

    public ResponseBuilder header(Header header, boolean overwrite) {
        this.headers.add(header, overwrite);
        return this;
    }

    public ResponseBuilder body(Publisher<ByteBuffer> body) {
        return body(new Content.From(body));
    }

    public ResponseBuilder body(Content body) {
        this.body = body;
        this.body.size().ifPresent(val -> header(new ContentLength(val), true));
        return this;
    }

    public ResponseBuilder body(byte[] body) {
        return this.body(new Content.From(body));
    }

    public ResponseBuilder textBody(String text) {
        return textBody(text, StandardCharsets.UTF_8);
    }

    public ResponseBuilder textBody(String text, Charset charset) {
        this.body = new Content.From(text.getBytes(charset));
        this.headers.add(ContentType.text(charset))
            .add(new ContentLength(body.size().orElseThrow()));
        return this;
    }

    public ResponseBuilder jsonBody(JsonStructure json) {
        return jsonBody(json, StandardCharsets.UTF_8);
    }

    public ResponseBuilder jsonBody(String json) {
        return jsonBody(json, StandardCharsets.UTF_8);
    }

    public ResponseBuilder jsonBody(JsonStructure json, Charset charset) {
        return jsonBody(json.toString(), charset);
    }

    public ResponseBuilder jsonBody(String json, Charset charset) {
        this.body = new Content.From(json.getBytes(charset));
        headers.add(ContentType.json(charset))
            .add(new ContentLength(body.size().orElseThrow())
        );
        return this;
    }

    public ResponseBuilder yamlBody(String yaml) {
        return yamlBody(yaml, StandardCharsets.UTF_8);
    }

    public ResponseBuilder yamlBody(String yaml, Charset charset) {
        this.body = new Content.From(yaml.getBytes(charset));
        this.headers.add(ContentType.yaml(charset))
            .add(new ContentLength(body.size().orElseThrow()));
        return this;
    }

    public ResponseBuilder htmlBody(String html, Charset charset) {
        this.body = new Content.From(html.getBytes(charset));
        this.headers.add(ContentType.html(charset))
            .add(new ContentLength(body.size().orElseThrow()));
        return this;
    }

    public ResponseImpl build() {
        if (headers.isEmpty() && body == Content.EMPTY) {
            return switch (status) {
                case CONTINUE -> RSP_CONTINUE;
                case OK -> RSP_OK;
                case CREATED -> RSP_CREATED;
                case ACCEPTED -> RSP_ACCEPTED;
                case NO_CONTENT -> RSP_NO_CONTENT;
                case MOVED_PERMANENTLY -> RSP_MOVED_PERMANENTLY;
                case MOVED_TEMPORARILY -> RSP_MOVED_TEMPORARILY;
                case NOT_MODIFIED -> RSP_NOT_MODIFIED;
                case TEMPORARY_REDIRECT -> RSP_TEMPORARY_REDIRECT;
                case BAD_REQUEST -> RSP_BAD_REQUEST;
                case UNAUTHORIZED -> RSP_UNAUTHORIZED;
                case FORBIDDEN -> RSP_FORBIDDEN;
                case NOT_FOUND -> RSP_NOT_FOUND;
                case METHOD_NOT_ALLOWED -> RSP_METHOD_NOT_ALLOWED;
                case REQUEST_TIMEOUT -> RSP_REQUEST_TIMEOUT;
                case CONFLICT -> RSP_CONFLICT;
                case LENGTH_REQUIRED -> RSP_LENGTH_REQUIRED;
                case REQUEST_TOO_LONG -> RSP_REQUEST_TOO_LONG;
                case REQUESTED_RANGE_NOT_SATISFIABLE -> RSP_REQUESTED_RANGE_NOT_SATISFIABLE;
                case EXPECTATION_FAILED -> RSP_EXPECTATION_FAILED;
                case TOO_MANY_REQUESTS -> RSP_TOO_MANY_REQUESTS;
                case INTERNAL_ERROR -> RSP_INTERNAL_ERROR;
                case NOT_IMPLEMENTED -> RSP_NOT_IMPLEMENTED;
                case SERVICE_UNAVAILABLE -> RSP_SERVICE_UNAVAILABLE;
            };
        }
        return new ResponseImpl(status, new UnmodifiableHeaders(headers.asList()), body);
    }

    public CompletableFuture<ResponseImpl> completedFuture() {
        return CompletableFuture.completedFuture(build());
    }

    private final static ResponseImpl RSP_OK = new ResponseImpl(RsStatus.OK, Headers.EMPTY, Content.EMPTY);
    private final static ResponseImpl RSP_NOT_FOUND = new ResponseImpl(RsStatus.NOT_FOUND, Headers.EMPTY, Content.EMPTY);
    private final static ResponseImpl RSP_CONTINUE = new ResponseImpl(RsStatus.CONTINUE, Headers.EMPTY, Content.EMPTY);
    private final static ResponseImpl RSP_CREATED = new ResponseImpl(RsStatus.CREATED, Headers.EMPTY, Content.EMPTY);
    private final static ResponseImpl RSP_ACCEPTED = new ResponseImpl(RsStatus.ACCEPTED, Headers.EMPTY, Content.EMPTY);
    private final static ResponseImpl RSP_NO_CONTENT = new ResponseImpl(RsStatus.NO_CONTENT, Headers.EMPTY, Content.EMPTY);
    private final static ResponseImpl RSP_MOVED_PERMANENTLY = new ResponseImpl(RsStatus.MOVED_PERMANENTLY, Headers.EMPTY, Content.EMPTY);
    private final static ResponseImpl RSP_MOVED_TEMPORARILY = new ResponseImpl(RsStatus.MOVED_TEMPORARILY, Headers.EMPTY, Content.EMPTY);
    private final static ResponseImpl RSP_NOT_MODIFIED = new ResponseImpl(RsStatus.NOT_MODIFIED, Headers.EMPTY, Content.EMPTY);
    private final static ResponseImpl RSP_TEMPORARY_REDIRECT = new ResponseImpl(RsStatus.TEMPORARY_REDIRECT, Headers.EMPTY, Content.EMPTY);
    private final static ResponseImpl RSP_BAD_REQUEST = new ResponseImpl(RsStatus.BAD_REQUEST, Headers.EMPTY, Content.EMPTY);
    private final static ResponseImpl RSP_UNAUTHORIZED = new ResponseImpl(RsStatus.UNAUTHORIZED, Headers.EMPTY, Content.EMPTY);
    private final static ResponseImpl RSP_FORBIDDEN = new ResponseImpl(RsStatus.FORBIDDEN, Headers.EMPTY, Content.EMPTY);
    private final static ResponseImpl RSP_METHOD_NOT_ALLOWED = new ResponseImpl(RsStatus.METHOD_NOT_ALLOWED, Headers.EMPTY, Content.EMPTY);
    private final static ResponseImpl RSP_REQUEST_TIMEOUT = new ResponseImpl(RsStatus.REQUEST_TIMEOUT, Headers.EMPTY, Content.EMPTY);
    private final static ResponseImpl RSP_CONFLICT = new ResponseImpl(RsStatus.CONFLICT, Headers.EMPTY, Content.EMPTY);
    private final static ResponseImpl RSP_LENGTH_REQUIRED = new ResponseImpl(RsStatus.LENGTH_REQUIRED, Headers.EMPTY, Content.EMPTY);
    private final static ResponseImpl RSP_REQUEST_TOO_LONG = new ResponseImpl(RsStatus.REQUEST_TOO_LONG, Headers.EMPTY, Content.EMPTY);
    private final static ResponseImpl RSP_REQUESTED_RANGE_NOT_SATISFIABLE = new ResponseImpl(RsStatus.REQUESTED_RANGE_NOT_SATISFIABLE, Headers.EMPTY, Content.EMPTY);
    private final static ResponseImpl RSP_EXPECTATION_FAILED = new ResponseImpl(RsStatus.EXPECTATION_FAILED, Headers.EMPTY, Content.EMPTY);
    private final static ResponseImpl RSP_TOO_MANY_REQUESTS = new ResponseImpl(RsStatus.TOO_MANY_REQUESTS, Headers.EMPTY, Content.EMPTY);
    private final static ResponseImpl RSP_INTERNAL_ERROR = new ResponseImpl(RsStatus.INTERNAL_ERROR, Headers.EMPTY, Content.EMPTY);
    private final static ResponseImpl RSP_NOT_IMPLEMENTED = new ResponseImpl(RsStatus.NOT_IMPLEMENTED, Headers.EMPTY, Content.EMPTY);
    private final static ResponseImpl RSP_SERVICE_UNAVAILABLE = new ResponseImpl(RsStatus.SERVICE_UNAVAILABLE, Headers.EMPTY, Content.EMPTY);
}
