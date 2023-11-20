/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm.http;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import com.artipie.npm.PackageNameFromUrl;
import com.artipie.scheduling.ArtifactEvent;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.reactivestreams.Publisher;

/**
 * Slice to handle `npm unpublish` command requests.
 * Request line to this slice looks like `/[<@scope>/]pkg/-rev/undefined`.
 * It unpublishes the whole package or a single version of package
 * when only one version is published.
 * @since 0.8
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
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final RequestLineFrom rqline = new RequestLineFrom(line);
        final String uri = rqline.uri().getPath();
        final Matcher matcher = UnpublishForceSlice.PTRN.matcher(uri);
        final Response resp;
        if (matcher.matches()) {
            final String pkg = new PackageNameFromUrl(
                String.format(
                    "%s %s %s", rqline.method(),
                    uri.substring(0, uri.indexOf("/-rev/")),
                    rqline.version()
                )
            ).value();
            CompletionStage<Void> res = this.storage.deleteAll(new Key.From(pkg));
            if (this.events.isPresent()) {
                res = res.thenRun(
                    () -> this.events.map(
                        queue -> queue.add(
                            new ArtifactEvent(UploadSlice.REPO_TYPE, this.rname, pkg)
                        )
                    )
                );
            }
            resp = new AsyncResponse(res.thenApply(nothing -> StandardRs.OK));
        } else {
            resp = new RsWithStatus(RsStatus.BAD_REQUEST);
        }
        return resp;
    }
}
