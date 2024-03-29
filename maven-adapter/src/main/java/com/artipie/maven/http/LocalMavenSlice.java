/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Meta;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.KeyLastPart;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.ResponseImpl;
import com.artipie.http.Slice;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.slice.KeyFromPath;
import com.artipie.maven.asto.RepositoryChecksums;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link Slice} based on a {@link Storage}. This is the main entrypoint
 * for dispatching GET requests for artifacts.
 */
final class LocalMavenSlice implements Slice {

    /**
     * All supported Maven artifacts according to
     * <a href="https://maven.apache.org/ref/3.6.3/maven-core/artifact-handlers.html">Artifact
     * handlers</a> by maven-core, and additionally {@code xml} metadata files are also artifacts.
     */
    private static final Pattern PTN_ARTIFACT =
        Pattern.compile(String.format(".+\\.(?:%s|xml)", String.join("|", MavenSlice.EXT)));

    /**
     * Repository storage.
     */
    private final Storage storage;

    /**
     * New local {@code GET} slice.
     *
     * @param storage Repository storage
     */
    LocalMavenSlice(Storage storage) {
        this.storage = storage;
    }

    @Override
    public CompletableFuture<ResponseImpl> response(RequestLine line, Headers headers, Content body) {
        final Key key = new KeyFromPath(line.uri().getPath());
        final Matcher match = LocalMavenSlice.PTN_ARTIFACT.matcher(new KeyLastPart(key).get());
        return match.matches()
            ? artifactResponse(line.method(), key)
            : plainResponse(line.method(), key);
    }

    /**
     * Artifact response for repository artifact request.
     * @param method Method
     * @param artifact Artifact key
     * @return Response
     */
    private CompletableFuture<ResponseImpl> artifactResponse(final RqMethod method, final Key artifact) {
        return switch (method) {
            case GET -> storage.exists(artifact)
                .thenApply(
                    exists -> {
                        if (exists) {
                            return storage.value(artifact)
                                .thenCombine(
                                    new RepositoryChecksums(storage).checksums(artifact),
                                    (body, checksums) ->
                                        ResponseBuilder.ok()
                                            .headers(ArtifactHeaders.from(artifact, checksums))
                                            .body(body)
                                            .build()
                                );
                        }
                        return CompletableFuture.completedFuture(ResponseBuilder.notFound().build());
                    }
                ).thenCompose(Function.identity());
            case HEAD ->
//                new ArtifactHeadResponse(this.storage, artifact);
                storage.exists(artifact).thenApply(
                    exists -> {
                        if (exists) {
                            return new RepositoryChecksums(storage)
                                .checksums(artifact)
                                .thenApply(
                                    checksums -> ResponseBuilder.ok()
                                        .headers(ArtifactHeaders.from(artifact, checksums))
                                        .build()
                                );
                        }
                        return CompletableFuture.completedFuture(ResponseBuilder.notFound().build());
                    }
                ).thenCompose(Function.identity());
            default -> CompletableFuture.completedFuture(ResponseBuilder.methodNotAllowed().build());
        };
    }

    /**
     * Plain response for non-artifact requests.
     * @param method Request method
     * @param key Location
     * @return Response
     */
    private CompletableFuture<ResponseImpl> plainResponse(final RqMethod method, final Key key) {
        return switch (method) {
            case GET -> plainResponse(
                this.storage, key,
                () -> this.storage.value(key).thenApply(val -> ResponseBuilder.ok().body(val).build())
            );
            case HEAD -> plainResponse(this.storage, key,
                () -> this.storage.metadata(key)
                    .thenApply(
                        meta -> ResponseBuilder.ok()
                            .header(new ContentLength(meta.read(Meta.OP_SIZE).orElseThrow()))
                            .build()
                    )
            );
            default -> CompletableFuture.completedFuture(ResponseBuilder.methodNotAllowed().build());
        };
    }

    private static CompletableFuture<ResponseImpl> plainResponse(
        Storage storage, Key key, Supplier<CompletableFuture<ResponseImpl>> actual
    ) {
        return storage.exists(key)
            .thenApply(
                exists -> exists
                    ? actual.get()
                    : CompletableFuture.completedFuture(ResponseBuilder.notFound().build())
            ).thenCompose(Function.identity());

    }
}
