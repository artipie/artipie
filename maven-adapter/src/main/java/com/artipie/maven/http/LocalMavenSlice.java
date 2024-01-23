/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven.http;

import com.artipie.asto.Key;
import com.artipie.asto.Meta;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.KeyLastPart;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.slice.KeyFromPath;
import java.nio.ByteBuffer;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.reactivestreams.Publisher;

/**
 * A {@link Slice} based on a {@link Storage}. This is the main entrypoint
 * for dispatching GET requests for artifacts.
 *
 * @since 0.5
 * @todo #117:30min Add test to verify this class.
 *  Create integration test against local maven repository to download artifacts from
 *  Artipie Maven repository and verify that all HEAD and GET requests has correct headers.
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
    LocalMavenSlice(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public Response response(
        final String line, final Iterable<Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final RequestLineFrom rline = new RequestLineFrom(line);
        final Key key = new KeyFromPath(rline.uri().getPath());
        final Matcher match = LocalMavenSlice.PTN_ARTIFACT.matcher(new KeyLastPart(key).get());
        final Response response;
        if (match.matches()) {
            response = this.artifactResponse(rline.method(), key);
        } else {
            response = this.plainResponse(rline.method(), key);
        }
        return response;
    }

    /**
     * Artifact response for repository artifact request.
     * @param method Method
     * @param artifact Artifact key
     * @return Response
     */
    private Response artifactResponse(final RqMethod method, final Key artifact) {
        final Response response;
        switch (method) {
            case GET:
                response = new ArtifactGetResponse(this.storage, artifact);
                break;
            case HEAD:
                response = new ArtifactHeadResponse(this.storage, artifact);
                break;
            default:
                response = new RsWithStatus(RsStatus.METHOD_NOT_ALLOWED);
                break;
        }
        return response;
    }

    /**
     * Plain response for non-artifact requests.
     * @param method Request method
     * @param key Location
     * @return Response
     */
    private Response plainResponse(final RqMethod method, final Key key) {
        final Response response;
        switch (method) {
            case GET:
                response = new PlainResponse(
                    this.storage, key,
                    () -> new AsyncResponse(this.storage.value(key).thenApply(RsWithBody::new))
                );
                break;
            case HEAD:
                response = new PlainResponse(
                    this.storage, key,
                    () -> new AsyncResponse(
                        this.storage.metadata(key).thenApply(
                            meta -> new RsWithHeaders(
                                StandardRs.OK, new ContentLength(meta.read(Meta.OP_SIZE).get())
                            )
                        )
                    )
                );
                break;
            default:
                response = new RsWithStatus(RsStatus.METHOD_NOT_ALLOWED);
                break;
        }
        return response;
    }

    /**
     * Plain non-artifact response for key.
     * @since 0.10
     */
    private static final class PlainResponse extends Response.Wrap {

        /**
         * New plain response.
         * @param storage Storage
         * @param key Location
         * @param actual Actual response with body or not
         */
        PlainResponse(final Storage storage, final Key key,
            final Supplier<? extends Response> actual) {
            super(
                new AsyncResponse(
                    storage.exists(key).thenApply(
                        exists -> {
                            final Response res;
                            if (exists) {
                                res = actual.get();
                            } else {
                                res = StandardRs.NOT_FOUND;
                            }
                            return res;
                        }
                    )
                )
            );
        }
    }
}
