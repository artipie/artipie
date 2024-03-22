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
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.BaseResponse;
import com.artipie.http.slice.KeyFromPath;

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
    public Response response(RequestLine line, Headers headers, Content body) {
        final Key key = new KeyFromPath(line.uri().getPath());
        final Matcher match = LocalMavenSlice.PTN_ARTIFACT.matcher(new KeyLastPart(key).get());
        if (match.matches()) {
            return this.artifactResponse(line.method(), key);
        }
        return this.plainResponse(line.method(), key);
    }

    /**
     * Artifact response for repository artifact request.
     * @param method Method
     * @param artifact Artifact key
     * @return Response
     */
    private Response artifactResponse(final RqMethod method, final Key artifact) {
        return switch (method) {
            case GET -> new ArtifactGetResponse(this.storage, artifact);
            case HEAD -> new ArtifactHeadResponse(this.storage, artifact);
            default -> BaseResponse.methodNotAllowed();
        };
    }

    /**
     * Plain response for non-artifact requests.
     * @param method Request method
     * @param key Location
     * @return Response
     */
    private Response plainResponse(final RqMethod method, final Key key) {
        return switch (method) {
            case GET -> plainResponse(this.storage, key,
                () -> new AsyncResponse(
                    this.storage.value(key).thenApply(val -> BaseResponse.ok().body(val))
                )
            );
            case HEAD -> plainResponse(this.storage, key,
                () -> new AsyncResponse(
                    this.storage.metadata(key)
                        .thenApply(
                            meta -> BaseResponse.ok().header(new ContentLength(meta.read(Meta.OP_SIZE).orElseThrow()))
                        )
                )
            );
            default -> BaseResponse.methodNotAllowed();
        };
    }

    private static Response plainResponse(
        Storage storage, Key key, Supplier<? extends Response> actual
    ) {
        return new AsyncResponse(
            storage.exists(key)
                .thenApply(exists -> exists ? actual.get() : BaseResponse.notFound())
        );
    }
}
