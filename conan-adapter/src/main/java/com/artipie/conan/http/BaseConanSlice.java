/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package  com.artipie.conan.http;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.ContentDigest;
import com.artipie.asto.ext.Digests;
import com.artipie.conan.Completables;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqHeaders;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.StandardRs;
import io.vavr.Tuple2;
import org.reactivestreams.Publisher;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base slice class for Conan REST APIs.
 * @since 0.1
 */
abstract class BaseConanSlice implements Slice {

    /**
     * Error message string for the client.
     */
    private static final String URI_S_NOT_FOUND = "URI %s not found. Handler: %s";

    /**
     * HTTP Content-type header name.
     */
    private static final String CONTENT_TYPE = "Content-Type";

    /**
     * Current Artipie storage instance.
     */
    private final Storage storage;

    /**
     * Request path wrapper object, corresponding to this Slice instance.
     */
    private final PathWrap pathwrap;

    /**
     * Ctor.
     * @param storage Current Artipie storage instance.
     * @param pathwrap Current path wrapper instance.
     */
    BaseConanSlice(final Storage storage, final PathWrap pathwrap) {
        this.storage = storage;
        this.pathwrap = pathwrap;
    }

    @Override
    public Response response(
        final RequestLine line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final String hostname = new RqHeaders.Single(headers, "Host").asString();
        final Matcher matcher = this.pathwrap.getPattern().matcher(line.uri().getPath());
        final CompletableFuture<RequestResult> content;
        if (matcher.matches()) {
            content = this.getResult(line, hostname, matcher);
        } else {
            content = CompletableFuture.completedFuture(new RequestResult());
        }
        return new AsyncResponse(
            content.thenApply(
                data -> {
                    final Response result;
                    if (data.isEmpty()) {
                        result = new RsWithBody(
                            StandardRs.NOT_FOUND,
                            String.format(
                                BaseConanSlice.URI_S_NOT_FOUND, line.uri(), this.getClass()
                            ),
                            StandardCharsets.UTF_8
                        );
                    } else {
                        result = new RsWithHeaders(
                            new RsWithBody(StandardRs.OK, data.getData()),
                            BaseConanSlice.CONTENT_TYPE, data.getType()
                        );
                    }
                    return result;
                }
            )
        );
    }

    /**
     * Returns current Artipie storage instance.
     * @return Storage object instance.
     */
    protected Storage getStorage() {
        return this.storage;
    }

    /**
     * Generates An md5 hash for package file.
     * @param key Storage key for package file.
     * @return An md5 hash string for file content.
     */
    protected CompletableFuture<String> generateMDhash(final Key key) {
        return this.storage.exists(key).thenCompose(
            exist -> {
                final CompletableFuture<String> result;
                if (exist) {
                    result = this.storage.value(key).thenCompose(
                        content -> new ContentDigest(content, Digests.MD5).hex()
                    );
                } else {
                    result = CompletableFuture.completedFuture("");
                }
                return result;
            });
    }

    /**
     * Processess the request and returns result data for this request.
     * @param request Artipie request line helper object instance.
     * @param hostname Current server host name string to construct and process URLs.
     * @param matcher Matched pattern matcher object for the current path wrapper.
     * @return Future object, providing request result data.
     */
    protected abstract CompletableFuture<RequestResult> getResult(
        RequestLine request, String hostname, Matcher matcher
    );

    /**
     * Generate RequestResult based on array of keys (files) and several handlers.
     * @param keys Array of keys to process.
     * @param mapper Mapper of key to the tuple with key & completable future.
     * @param generator Filters and generates value for json.
     * @param ctor Constructs resulting json string.
     * @param <T> Generators result type.
     * @return Json RequestResult in CompletableFuture.
     */
    protected static <T> CompletableFuture<RequestResult> generateJson(
        final String[] keys,
        final Function<String, Tuple2<Key, CompletableFuture<T>>> mapper,
        final Function<Tuple2<String, T>, Optional<String>> generator,
        final Function<JsonObjectBuilder, String> ctor
    ) {
        final List<Tuple2<Key, CompletableFuture<T>>> keychecks = Stream.of(keys).map(mapper)
            .collect(Collectors.toList());
        return new Completables.JoinTuples<>(keychecks).toTuples().thenApply(
            tuples -> {
                final JsonObjectBuilder builder = Json.createObjectBuilder();
                for (final Tuple2<Key, T> tuple : tuples) {
                    final Optional<String> result = generator.apply(
                        new Tuple2<>(tuple._1().string(), tuple._2())
                    );
                    if (result.isPresent()) {
                        final String[] parts = tuple._1().string().split("/");
                        builder.add(parts[parts.length - 1], result.get());
                    }
                }
                return builder;
            }).thenApply(ctor).thenApply(RequestResult::new);
    }

    /**
     * HTTP Request result bytes + Content-type string.
     * @since 0.1
     */
    protected static final class RequestResult {

        /**
         * Request result data bytes.
         */
        private final byte[] data;

        /**
         * Request result Content-type.
         */
        private final String type;

        /**
         * Initializes object with data bytes array and Content-Type string.
         * @param data Request result data bytes.
         * @param type Request result Content-type.
         */
        public RequestResult(final byte[] data, final String type) {
            this.data = Arrays.copyOf(data, data.length);
            this.type = type;
        }

        /**
         * Initializes object with data string, and json content type.
         * @param data Result data as String.
         */
        public RequestResult(final String data) {
            this(data.getBytes(StandardCharsets.UTF_8), "application/json");
        }

        /**
         * Initializes object with empty string, and json content type.
         */
        public RequestResult() {
            this("");
        }

        /**
         * Returns response data bytes.
         * @return Respose data as array of bytes.
         */
        public byte[] getData() {
            return this.data.clone();
        }

        /**
         * Returns response Content-type string.
         * @return Respose Content-type as String.
         */
        public String getType() {
            return this.type;
        }

        /**
         * Checks if data is empty.
         * @return True, if data is empty.
         */
        public boolean isEmpty() {
            return this.data.length == 0;
        }
    }
}
