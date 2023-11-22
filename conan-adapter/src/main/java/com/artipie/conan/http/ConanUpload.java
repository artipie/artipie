/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package  com.artipie.conan.http;

import com.artipie.ArtipieException;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.conan.ItemTokenizer;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rq.RqHeaders;
import com.artipie.http.rq.RqParams;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.slice.SliceUpload;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.stream.JsonParser;
import org.reactivestreams.Publisher;

/**
 * Slice for Conan package data uploading support.
 * @since 0.1
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
     * @param line Request line.
     * @param pathwrap Wrapper object for Conan protocol request path.
     * @return Corresponding matcher for the request.
     */
    private static Matcher matchRequest(final String line, final PathWrap pathwrap) {
        final Matcher matcher = pathwrap.getPattern().matcher(
            new RequestLineFrom(line).uri().getPath()
        );
        if (!matcher.matches()) {
            throw new ArtipieException(
                String.join("Request parameters doesn't match: ", line)
            );
        }
        return matcher;
    }

    /**
     * Generates error message for the requested file name.
     * @param filename Requested file name.
     * @return Error message for the response.
     */
    private static CompletableFuture<Response> generateError(final String filename) {
        return CompletableFuture.completedFuture(
            new RsWithBody(
                StandardRs.NOT_FOUND,
                String.format(ConanUpload.URI_S_NOT_FOUND, filename),
                StandardCharsets.UTF_8
            )
        );
    }

    /**
     * Conan /v1/conans/{path}/upload_urls REST APIs.
     * @since 0.1
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
         * Ctor.
         *
         * @param storage Current Artipie storage instance.
         * @param tokenizer Tokenizer for repository items.
         */
        public UploadUrls(final Storage storage, final ItemTokenizer tokenizer) {
            this.storage = storage;
            this.tokenizer = tokenizer;
        }

        @Override
        public Response response(final String line,
            final Iterable<Map.Entry<String, String>> headers, final Publisher<ByteBuffer> body) {
            final Matcher matcher = matchRequest(line, ConanUpload.UPLOAD_SRC_PATH);
            final String path = matcher.group(ConanUpload.URI_PATH);
            final String hostname = new RqHeaders.Single(headers, ConanUpload.HOST).asString();
            return new AsyncResponse(
                this.storage.exists(new Key.From(path)).thenCompose(
                    exist -> {
                        final CompletableFuture<Response> result;
                        if (exist) {
                            result = generateError(path);
                        } else {
                            result = this.generateUrls(body, path, hostname);
                        }
                        return result;
                    }
                )
            );
        }

        /**
         * Implements uploading from the client to server repository storage.
         * @param body Request body with file data.
         * @param path Target path for the package.
         * @param hostname Server host name.
         * @return Respose result of this operation.
         */
        private CompletableFuture<Response> generateUrls(final Publisher<ByteBuffer> body,
            final String path, final String hostname) {
            return new PublisherAs(body).asciiString().thenApply(
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
                    return (Response) new RsWithHeaders(
                        new RsWithBody(
                            StandardRs.OK, result.build().toString(), StandardCharsets.UTF_8
                        ),
                        ConanUpload.CONTENT_TYPE, ConanUpload.JSON_TYPE
                    );
                }
            ).toCompletableFuture();
        }
    }

    /**
     * Conan HTTP PUT /{path/to/file}?signature={signature} REST API.
     * @since 0.1
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
        public Response response(final String line,
            final Iterable<Map.Entry<String, String>> headers, final Publisher<ByteBuffer> body) {
            final String path = new RequestLineFrom(line).uri().getPath();
            final String hostname = new RqHeaders.Single(headers, ConanUpload.HOST).asString();
            final Optional<String> token = new RqParams(
                new RequestLineFrom(line).uri().getQuery()
            ).value("signature");
            final Response response;
            if (token.isPresent()) {
                response = new AsyncResponse(
                    this.tokenizer.authenticateToken(token.get()).thenApply(
                        item -> {
                            final Response resp;
                            if (item.isPresent() && item.get().getHostname().equals(hostname)
                                && item.get().getPath().equals(path)) {
                                resp = new SliceUpload(this.storage).response(line, headers, body);
                            } else {
                                resp = new RsWithStatus(RsStatus.UNAUTHORIZED);
                            }
                            return resp;
                        }
                    )
                );
            } else {
                response = new RsWithStatus(RsStatus.UNAUTHORIZED);
            }
            return response;
        }
    }
}
