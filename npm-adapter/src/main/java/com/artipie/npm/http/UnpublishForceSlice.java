/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RequestLine;
import com.artipie.npm.PackageNameFromUrl;
import com.artipie.scheduling.ArtifactEvent;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Slice to handle `npm unpublish` command requests.
 * Request line to this slice looks like `/[<@scope>/]pkg/-rev/undefined`.
 * It unpublishes the whole package or a single version of package
 * when only one version is published.
 */
final class UnpublishForceSlice implements Slice {
    /**
     * Endpoint request line pattern.
     */
    static final Pattern PTRN = Pattern.compile("/.*/-rev/.*$");

    /**
     * Abstract Storage.
     */
    private final Storage storage;

    /**
     * Artifact events queue.
     */
    private final Optional<Queue<ArtifactEvent>> events;

    /**
     * Repository name.
     */
    private final String rname;

    /**
     * Ctor.
     * @param storage Abstract storage
     * @param events Events queue
     * @param rname Repository name
     */
    UnpublishForceSlice(final Storage storage, final Optional<Queue<ArtifactEvent>> events,
        final String rname) {
        this.storage = storage;
        this.events = events;
        this.rname = rname;
    }

    @Override
    public CompletableFuture<Response> response(
        final RequestLine line,
        final Headers headers,
        final Content body
    ) {
        final String uri = line.uri().getPath();
        final Matcher matcher = UnpublishForceSlice.PTRN.matcher(uri);
        if (matcher.matches()) {
            final String pkg = new PackageNameFromUrl(
                String.format(
                    "%s %s %s", line.method(),
                    uri.substring(0, uri.indexOf("/-rev/")),
                    line.version()
                )
            ).value();
            CompletableFuture<Void> res = this.storage.deleteAll(new Key.From(pkg));
            if (this.events.isPresent()) {
                res = res.thenRun(
                    () -> this.events.map(
                        queue -> queue.add(
                            new ArtifactEvent(UploadSlice.REPO_TYPE, this.rname, pkg)
                        )
                    )
                );
            }
            return res.thenApply(nothing -> ResponseBuilder.ok().build());
        }
        return ResponseBuilder.badRequest().completedFuture();
    }
}
