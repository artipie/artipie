/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.rs;

import com.artipie.asto.Content;
import com.artipie.http.Connection;
import com.artipie.http.Headers;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.headers.ContentType;
import com.artipie.http.headers.Header;
import org.reactivestreams.Publisher;

import javax.json.JsonStructure;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionStage;

public class BaseResponse implements com.artipie.http.Response {

    public static BaseResponse from(RsStatus status) {
        return new BaseResponse(status);
    }

    public static BaseResponse ok() {
        return new BaseResponse(RsStatus.OK);
    }

    public static BaseResponse created() {
        return new BaseResponse(RsStatus.CREATED);
    }

    public static BaseResponse accepted() {
        return new BaseResponse(RsStatus.ACCEPTED);
    }

    public static BaseResponse temporaryRedirect() {
        return new BaseResponse(RsStatus.TEMPORARY_REDIRECT);
    }

    public static BaseResponse movedPermanently() {
        return new BaseResponse(RsStatus.MOVED_PERMANENTLY);
    }

    public static BaseResponse notFound() {
        return new BaseResponse(RsStatus.NOT_FOUND);
    }

    public static BaseResponse noContent() {
        return new BaseResponse(RsStatus.NO_CONTENT);
    }

    public static BaseResponse unavailable() {
        return new BaseResponse(RsStatus.UNAVAILABLE);
    }

    public static BaseResponse unauthorized() {
        return new BaseResponse(RsStatus.UNAUTHORIZED);
    }

    public static BaseResponse forbidden() {
        return new BaseResponse(RsStatus.FORBIDDEN);
    }

    public static BaseResponse methodNotAllowed() {
        return new BaseResponse(RsStatus.METHOD_NOT_ALLOWED);
    }

    public static BaseResponse badRequest() {
        return new BaseResponse(RsStatus.BAD_REQUEST);
    }

    public static BaseResponse badRequest(Throwable error) {
        return new BaseResponse(RsStatus.BAD_REQUEST)
            .body(errorBody(error));
    }

    public static BaseResponse payloadTooLarge() {
        return new BaseResponse(RsStatus.PAYLOAD_TOO_LARGE);
    }

    public static BaseResponse internalError() {
        return new BaseResponse(RsStatus.INTERNAL_ERROR);
    }

    public static BaseResponse internalError(Throwable error) {
        return new BaseResponse(RsStatus.INTERNAL_ERROR)
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

    BaseResponse(RsStatus status) {
        this.status = status;
        this.headers = new Headers();
        this.body = Content.EMPTY;
    }

    public BaseResponse headers(Headers headers) {
        this.headers = headers.copy();
        return this;
    }

    public BaseResponse header(String name, String val) {
        this.headers.add(name, val);
        return this;
    }

    public BaseResponse header(Header header) {
        this.headers.add(header, false);
        return this;
    }

    public BaseResponse header(Header header, boolean overwrite) {
        this.headers.add(header, overwrite);
        return this;
    }

    public BaseResponse body(Publisher<ByteBuffer> body) {
        return body(new Content.From(body));
    }

    public BaseResponse body(Content body) {
        this.body = body;
        this.body.size().ifPresent(val -> header(new ContentLength(val), true));
        return this;
    }

    public BaseResponse body(byte[] body) {
        return this.body(new Content.From(body));
    }

    public BaseResponse textBody(String text) {
        return textBody(text, StandardCharsets.UTF_8);
    }

    public BaseResponse textBody(String text, Charset charset) {
        this.body = new Content.From(text.getBytes(charset));
        this.headers = Headers.from(
            ContentType.text(charset),
            new ContentLength(body.size().orElseThrow())
        );
        return this;
    }

    public BaseResponse jsonBody(JsonStructure json) {
        return jsonBody(json, StandardCharsets.UTF_8);
    }

    public BaseResponse jsonBody(String json) {
        return jsonBody(json, StandardCharsets.UTF_8);
    }

    public BaseResponse jsonBody(JsonStructure json, Charset charset) {
        return jsonBody(json.toString(), charset);
    }

    public BaseResponse jsonBody(String json, Charset charset) {
        this.body = new Content.From(json.getBytes(charset));
        this.headers = Headers.from(
            ContentType.json(charset),
            new ContentLength(body.size().orElseThrow())
        );
        return this;
    }

    public BaseResponse yamlBody(String yaml) {
        return yamlBody(yaml, StandardCharsets.UTF_8);
    }

    public BaseResponse yamlBody(String yaml, Charset charset) {
        this.body = new Content.From(yaml.getBytes(charset));
        this.headers = Headers.from(
            ContentType.yaml(charset),
            new ContentLength(body.size().orElseThrow())
        );
        return this;
    }

    public BaseResponse htmlBody(String html, Charset charset) {
        this.body = new Content.From(html.getBytes(charset));
        this.headers = Headers.from(
            ContentType.html(charset),
            new ContentLength(body.size().orElseThrow())
        );
        return this;
    }

    @Override
    public CompletionStage<Void> send(Connection connection) {
        return connection.accept(status, headers, body);
    }
}
