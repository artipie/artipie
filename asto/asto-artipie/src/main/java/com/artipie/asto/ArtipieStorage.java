/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto;

import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.client.UriClientSlice;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.slice.ContentWithSize;

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

    @Override
    public CompletableFuture<Boolean> exists(final Key key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletableFuture<Collection<Key>> list(final Key prefix) {
        return this.remote.response(
            new RequestLine(RqMethod.GET, "/" + prefix),
            Headers.from("Accept", "application/json"),
            Content.EMPTY
        ).<CompletableFuture<Collection<Key>>>thenApply(response -> {
            if (response.status().success()) {
                return response.body().asStringFuture()
                    .thenApply(ArtipieStorage::parse);
            }
            return CompletableFuture.failedFuture(
                new ArtipieIOException(
                    String.format(
                        "Cannot get lists blobs contained in given path [prefix=%s, status=%s]",
                        prefix, response.status()
                    )
                )
            );
        }).thenCompose(Function.identity());
    }

    @Override
    public CompletableFuture<Void> save(final Key key, final Content content) {
        return this.remote.response(
            new RequestLine(RqMethod.PUT,  "/" + key),
            Headers.from(new ContentLength(content.size().orElseThrow())),
            content
        ).thenCompose(response -> {
            if (response.status().success()) {
                return CompletableFuture.completedFuture(null);
            }
            return CompletableFuture.failedFuture(
                new ArtipieIOException(
                    String.format(
                        "Entry is not created [key=%s, status=%s]",
                        key, response.status()
                    )
                )
            );
        });
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
        return this.remote.response(
            new RequestLine(RqMethod.GET, "/" + key), Headers.EMPTY, Content.EMPTY
        ).thenCompose(resp -> {
            CompletableFuture<Content> res;
            if (resp.status().success()) {
                res = CompletableFuture.completedFuture(
                    new ContentWithSize(resp.body(), resp.headers())
                    );
                } else {
                res = CompletableFuture.failedFuture(
                    new ArtipieIOException(String.format("Cannot get a value [key=%s, status=%s]",
                        key, resp.status()))
                    );
                }
            return res;
        });
    }

    @Override
    public CompletableFuture<Void> delete(final Key key) {
        return this.remote.response(new RequestLine(RqMethod.DELETE, "/" + key),
            Headers.EMPTY, Content.EMPTY
        ).thenCompose(
            resp -> {
                if (resp.status().success()) {
                    return CompletableFuture.completedFuture(null);
                }
                return CompletableFuture.failedFuture(
                    new ArtipieIOException(String.format("Entry is not deleted [key=%s, status=%s]",
                        key, resp.status()))
                );
            }
        );
    }

    @Override
    public <T> CompletionStage<T> exclusively(Key key, Function<Storage, CompletionStage<T>> operation) {
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
}
