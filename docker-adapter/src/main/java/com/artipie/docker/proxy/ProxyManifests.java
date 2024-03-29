/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.proxy;

import com.artipie.asto.Content;
import com.artipie.asto.FailedCompletionStage;
import com.artipie.docker.Digest;
import com.artipie.docker.ManifestReference;
import com.artipie.docker.Manifests;
import com.artipie.docker.Repo;
import com.artipie.docker.RepoName;
import com.artipie.docker.Tag;
import com.artipie.docker.Tags;
import com.artipie.docker.http.DigestHeader;
import com.artipie.docker.manifest.JsonManifest;
import com.artipie.docker.manifest.Manifest;
import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.headers.Header;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.RsStatus;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Proxy implementation of {@link Repo}.
 */
public final class ProxyManifests implements Manifests {

    private static final Headers MANIFEST_ACCEPT_HEADERS = Headers.from(
            new Header("Accept", "application/json"),
            new Header("Accept", "application/vnd.oci.image.index.v1+json"),
            new Header("Accept", "application/vnd.oci.image.manifest.v1+json"),
            new Header("Accept", "application/vnd.docker.distribution.manifest.v1+prettyjws"),
            new Header("Accept", "application/vnd.docker.distribution.manifest.v2+json"),
            new Header("Accept", "application/vnd.docker.distribution.manifest.list.v2+json")
    );

    public static String uri(String repo, int limit, String from) {
        String lim = limit > 0 ? "n=" + limit : null;
        String last = Strings.isNullOrEmpty(from) ? null : "last=" + from;
        String params = Joiner.on("&").skipNulls().join(lim, last);

        return String.format("/v2/%s/tags/list%s",
            repo, Strings.isNullOrEmpty(params) ? "" : '?' + params
        );
    }

    /**
     * Remote repository.
     */
    private final Slice remote;

    /**
     * Repository name.
     */
    private final RepoName name;

    /**
     * Ctor.
     *
     * @param remote Remote repository.
     * @param name Repository name.
     */
    public ProxyManifests(final Slice remote, final RepoName name) {
        this.remote = remote;
        this.name = name;
    }

    @Override
    public CompletionStage<Manifest> put(final ManifestReference ref, final Content content) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CompletionStage<Optional<Manifest>> get(final ManifestReference ref) {
        return new ResponseSink<>(
            this.remote.response(
                new RequestLine(RqMethod.GET, new ManifestPath(this.name, ref).string()),
                MANIFEST_ACCEPT_HEADERS,
                Content.EMPTY
            ),
            response -> {
                final CompletionStage<Optional<Manifest>> result;
                if (response.status() == RsStatus.OK) {
                    final Digest digest = new DigestHeader(response.headers()).value();
                    result = response.body().asBytesFuture().thenApply(
                        bytes -> Optional.of(new JsonManifest(digest, bytes))
                    );
                } else if (response.status() == RsStatus.NOT_FOUND) {
                    result = CompletableFuture.completedFuture(Optional.empty());
                } else {
                    result = unexpected(response.status());
                }
                return result;
            }
        ).result();
    }

    @Override
    public CompletionStage<Tags> tags(final Optional<Tag> from, final int limit) {
        String fromStr = from.map(Tag::value).orElse(null);
        return new ResponseSink<>(
            this.remote.response(
                new RequestLine(
                    RqMethod.GET, uri(name.value(), limit, fromStr)
                ),
                Headers.EMPTY,
                Content.EMPTY
            ),
            response -> {
                final CompletionStage<Tags> result;
                if (response.status() == RsStatus.OK) {
                    result = CompletableFuture.completedFuture(response::body);
                } else {
                    result = unexpected(response.status());
                }
                return result;
            }
        ).result();
    }

    /**
     * Creates completion stage failed with unexpected status exception.
     *
     * @param status Status to be reported in error.
     * @param <T> Completion stage result type.
     * @return Failed completion stage.
     */
    private static <T> CompletionStage<T> unexpected(final RsStatus status) {
        return new FailedCompletionStage<>(
            new IllegalArgumentException(String.format("Unexpected status: %s", status))
        );
    }
}
