/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package  com.artipie.conan.http;

import com.artipie.ArtipieException;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.conan.ItemTokenizer;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.ResponseImpl;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqHeaders;
import com.artipie.http.rq.RqParams;
import com.artipie.http.slice.SliceUpload;
import org.reactivestreams.Publisher;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.stream.JsonParser;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Matcher;

/**
 * Slice for Conan package data uploading support.
 */
public final class ConanUpload {

    /**
     * Pattern for /v1/conans/{path}/upload_urls.
     */
    public static final PathWrap UPLOAD_SRC_PATH = new PathWrap.UploadSrc();

    /**
     * Error message string for the client.
     */
    private static final String URI_S_NOT_FOUND = "URI %s not found.";

    /**
     * HTTP Content-type header name.
     */
    private static final String CONTENT_TYPE = "Content-Type";

    /**
     * HTTP json application type string.
     */
    private static final String JSON_TYPE = "application/json";

    /**
     * Path part of the request URI.
     */
    private static final String URI_PATH = "path";

    /**
     * Host name http header.
     */
    private static final String HOST = "Host";

    /**
     * Protocol type for download URIs.
     */
    private static final String PROTOCOL = "http://";

    /**
     * Subdir for package recipe (sources).
     */
    private static final String PKG_SRC_DIR = "/0/export/";

    /**
     * Subdir for package binary.
     */
    private static final String PKG_BIN_DIR = "/0/";

    /**
     * Ctor is hidden.
     */
    private ConanUpload() { }

    /**
     * Match pattern for the request.
     *
     * @param line Request line.
     * @return Corresponding matcher for the request.
     */
    private static Matcher matchRequest(final RequestLine line) {
        final Matcher matcher = ConanUpload.UPLOAD_SRC_PATH.getPattern().matcher(
            line.uri().getPath()
        );
        if (!matcher.matches()) {
            throw new ArtipieException("Request parameters doesn't match: " + line);
        }
        return matcher;
    }

    /**
     * Generates error message for the requested file name.
     * @param filename Requested file name.
     * @return Error message for the response.
     */
    private static CompletableFuture<ResponseImpl> generateError(final String filename) {
        return CompletableFuture.completedFuture(
            ResponseBuilder.notFound()
                .textBody(String.format(ConanUpload.URI_S_NOT_FOUND, filename))
                .build()
        );
    }

    /**
     * Conan /v1/conans/{path}/upload_urls REST APIs.
     */
    public static final class UploadUrls implements Slice {

        /**
         * Current Artipie storage instance.
         */
        private final Storage storage;

        /**
         * Tokenizer for repository items.
         */
        private final ItemTokenizer tokenizer;

        /**
         * @param storage Current Artipie storage instance.
         * @param tokenizer Tokenizer for repository items.
         */
        public UploadUrls(final Storage storage, final ItemTokenizer tokenizer) {
            this.storage = storage;
            this.tokenizer = tokenizer;
        }

        @Override
        public CompletableFuture<ResponseImpl> response(RequestLine line, Headers headers, Content body) {
            final Matcher matcher = matchRequest(line);
            final String path = matcher.group(ConanUpload.URI_PATH);
            final String hostname = new RqHeaders.Single(headers, ConanUpload.HOST).asString();
            return this.storage.exists(new Key.From(path))
                .thenCompose(
                    exist -> exist ? generateError(path) : generateUrls(body, path, hostname)
                );
        }

        /**
         * Implements uploading from the client to server repository storage.
         * @param body Request body with file data.
         * @param path Target path for the package.
         * @param hostname Server host name.
         * @return Respose result of this operation.
         */
        private CompletableFuture<ResponseImpl> generateUrls(final Publisher<ByteBuffer> body,
            final String path, final String hostname) {
            return new Content.From(body).asStringFuture()
                .thenApply(
                    str -> {
                        final JsonParser parser = Json.createParser(new StringReader(str));
                        parser.next();
                        final JsonObjectBuilder result = Json.createObjectBuilder();
                        for (final String key : parser.getObject().keySet()) {
                            final String pkgnew = "/_/_/packages/";
                            final int ipkg = path.indexOf(pkgnew);
                            final String fpath;
                            final String pkgdir;
                            if (ipkg > 0) {
                                fpath = path.replace(pkgnew, "/_/_/0/package/");
                                pkgdir = ConanUpload.PKG_BIN_DIR;
                            } else {
                                fpath = path;
                                pkgdir = ConanUpload.PKG_SRC_DIR;
                            }
                            final String filepath = String.join(
                                "", "/", fpath, pkgdir, key
                            );
                            final String url = String.join(
                                "", ConanUpload.PROTOCOL, hostname, filepath, "?signature=",
                                this.tokenizer.generateToken(filepath, hostname)
                            );
                            result.add(key, url);
                        }
                        return ResponseBuilder.ok()
                            .header(ConanUpload.CONTENT_TYPE, ConanUpload.JSON_TYPE)
                            .jsonBody(result.build())
                            .build();
                    }
                ).toCompletableFuture();
        }
    }

    /**
     * Conan HTTP PUT /{path/to/file}?signature={signature} REST API.
     */
    public static final class PutFile implements Slice {

        /**
         * Current Artipie storage instance.
         */
        private final Storage storage;

        /**
         * Tokenizer for repository items.
         */
        private final ItemTokenizer tokenizer;

        /**
         * Ctor.
         * @param storage Current Artipie storage instance.
         * @param tokenizer Tokenize repository items via JWT tokens.
         */
        public PutFile(final Storage storage, final ItemTokenizer tokenizer) {
            this.storage = storage;
            this.tokenizer = tokenizer;
        }

        @Override
        public CompletableFuture<ResponseImpl> response(RequestLine line, Headers headers, Content body) {
            final String path = line.uri().getPath();
            final String hostname = new RqHeaders.Single(headers, ConanUpload.HOST).asString();
            final Optional<String> token = new RqParams(line.uri().getQuery()).value("signature");
            if (token.isPresent()) {
                return this.tokenizer.authenticateToken(token.get())
                    .toCompletableFuture()
                    .thenApply(
                        item -> {
                            if (item.isPresent() && item.get().getHostname().equals(hostname)
                                && item.get().getPath().equals(path)) {
                                return new SliceUpload(this.storage)
                                    .response(line, headers, body);
                            }
                            return CompletableFuture.completedFuture(
                                ResponseBuilder.unauthorized().build()
                            );
                        }
                    ).thenCompose(Function.identity());
            }
            return CompletableFuture.completedFuture(
                ResponseBuilder.unauthorized().build()
            );
        }
    }
}
