/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.client.UriClientSlice;
import com.artipie.http.client.auth.AuthClientSlice;
import com.artipie.http.client.auth.Authenticator;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.slice.ContentWithSize;
import io.reactivex.Flowable;

import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonString;
import java.io.StringReader;
import java.net.URI;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Proxy storage for a file-adapter via HTTP.
 */
public final class ArtipieStorage implements Storage {

    /**
     * Remote slice.
     */
    private final Slice remote;

    /**
     * @param clients HTTP clients
     * @param remote Remote URI
     */
    public ArtipieStorage(final ClientSlices clients, final URI remote) {
        this(new UriClientSlice(clients, remote));
    }

    /**
     * @param remote Remote slice
     */
    ArtipieStorage(final Slice remote) {
        this.remote = remote;
    }

    /**
     * @param clients HTTP clients
     * @param remote Remote URI
     * @param auth Authenticator
     */
    public ArtipieStorage(ClientSlices clients, URI remote, Authenticator auth) {
        this(new AuthClientSlice(new UriClientSlice(clients, remote), auth));
    }

    @Override
    public CompletableFuture<Boolean> exists(final Key key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Collection<Key>> list(final Key prefix) {
        final CompletableFuture<Collection<Key>> promise = new CompletableFuture<>();
        this.remote.response(
            new RequestLine(RqMethod.GET, ArtipieStorage.uri(prefix)),
            new Headers.From("Accept", "application/json"),
            Content.EMPTY
        ).send(
            (status, rsheaders, rsbody) -> {
                final CompletableFuture<Void> term = new CompletableFuture<>();
                if (status.success()) {
                    new Content.From(
                        Flowable.fromPublisher(rsbody)
                            .doOnError(term::completeExceptionally)
                            .doOnTerminate(() -> term.complete(null))
                    ).asStringFuture().thenApply(s -> promise.complete(ArtipieStorage.parse(s)));
                } else {
                    promise.completeExceptionally(
                        new ArtipieIOException(
                            String.format(
                                "Cannot get lists blobs contained in given path [prefix=%s, status=%s]",
                                prefix, status
                            )
                        )
                    );
                }
                return term;
            }
        );
        return promise;
    }

    @Override
    public CompletableFuture<Void> save(final Key key, final Content content) {
        return this.remote.response(
            new RequestLine(RqMethod.PUT, ArtipieStorage.uri(key)),
            new Headers.From(new ContentLength(content.size().orElseThrow())),
            content
        ).send(
            (status, rsheaders, rsbody) -> {
                final CompletionStage<Void> res;
                if (status.success()) {
                    res = CompletableFuture.allOf();
                } else {
                    res = new FailedCompletionStage<>(
                        new ArtipieIOException(
                            String.format(
                                "Entry is not created [key=%s, status=%s]",
                                key, status
                            )
                        )
                    );
                }
                return res;
            }
        ).toCompletableFuture();
    }

    @Override
    public CompletableFuture<Void> move(final Key source, final Key destination) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<? extends Meta> metadata(final Key key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Content> value(final Key key) {
        final CompletableFuture<Content> promise = new CompletableFuture<>();
        this.remote.response(
            new RequestLine(RqMethod.GET, ArtipieStorage.uri(key)),
            Headers.EMPTY,
            Content.EMPTY
        ).send(
            (status, rsheaders, rsbody) -> {
                final CompletableFuture<Void> term = new CompletableFuture<>();
                if (status.success()) {
                    promise.complete(
                        new ContentWithSize(rsbody, rsheaders)
                    );
                } else {
                    promise.completeExceptionally(
                        new ArtipieIOException(
                            String.format(
                                "Cannot get a value [key=%s, status=%s]",
                                key, status
                            )
                        )
                    );
                }
                return term;
            }
        );
        return promise;
    }

    @Override
    public CompletableFuture<Void> delete(final Key key) {
        return this.remote.response(
            new RequestLine(RqMethod.DELETE, ArtipieStorage.uri(key)),
            Headers.EMPTY,
            Content.EMPTY
        ).send(
            (status, rsheaders, rsbody) -> {
                final CompletionStage<Void> res;
                if (status.success()) {
                    res = CompletableFuture.allOf();
                } else {
                    res = new FailedCompletionStage<>(
                        new ArtipieIOException(
                            String.format(
                                "Entry is not deleted [key=%s, status=%s]",
                                key, status
                            )
                        )
                    );
                }
                return res;
            }
        ).toCompletableFuture();
    }

    @Override
    public <T> CompletionStage<T> exclusively(
        final Key key,
        final Function<Storage, CompletionStage<T>> operation) {
        throw new UnsupportedOperationException();
    }

    /**
     * Parse JSON array of keys.
     *
     * @param json JSON array of keys.
     * @return Collection of keys.
     */
    private static Collection<Key> parse(final String json) {
        try (
            JsonReader reader = Json.createReader(new StringReader(json))
        ) {
            return reader.readArray()
                .stream()
                .map(v -> (JsonString) v)
                .map(js -> new Key.From(js.getString()))
                .collect(Collectors.toList());
        }
    }

    /**
     * Converts key to URI string.
     *
     * @param key Key.
     * @return URI string.
     */
    private static String uri(final Key key) {
        return String.format("/%s", key);
    }
}
