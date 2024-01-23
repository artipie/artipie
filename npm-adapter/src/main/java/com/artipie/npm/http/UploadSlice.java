/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.npm.http;

import com.artipie.asto.Concatenation;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Remaining;
import com.artipie.asto.Storage;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.Login;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.npm.PackageNameFromUrl;
import com.artipie.npm.Publish;
import com.artipie.scheduling.ArtifactEvent;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import org.reactivestreams.Publisher;

/**
 * UploadSlice.
 *
 * @since 0.1
 */
public final class UploadSlice implements Slice {

    /**
     * Repository type.
     */
    public static final String REPO_TYPE = "npm";

    /**
     * The npm publish front.
     */
    private final Publish npm;

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
     *
     * @param npm Npm publish front
     * @param storage Abstract storage
     * @param events Artifact events queue
     * @param rname Repository name
         */
    public UploadSlice(final Publish npm, final Storage storage,
        final Optional<Queue<ArtifactEvent>> events, final String rname) {
        this.npm = npm;
        this.storage = storage;
        this.events = events;
        this.rname = rname;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final String pkg = new PackageNameFromUrl(line).value();
        final Key uploaded = new Key.From(
            String.format(
                "%s-%s-uploaded",
                pkg,
                UUID.randomUUID().toString()
            )
        );
        return new AsyncResponse(
            new Concatenation(body).single()
                .map(Remaining::new)
                .map(Remaining::bytes)
                .to(SingleInterop.get())
                .thenCompose(bytes -> this.storage.save(uploaded, new Content.From(bytes)))
                .thenCompose(
                    ignored -> this.events.map(
                        queue ->  this.npm.publishWithInfo(new Key.From(pkg), uploaded).thenAccept(
                            info -> this.events.get().add(
                                new ArtifactEvent(
                                    UploadSlice.REPO_TYPE, this.rname,
                                    new Login(new Headers.From(headers)).getValue(),
                                    info.packageName(), info.packageVersion(), info.tarSize()
                                )
                            )
                        )
                    ).orElseGet(() -> this.npm.publish(new Key.From(pkg), uploaded))
                )
                .thenCompose(ignored -> this.storage.delete(uploaded))
                .thenApply(ignored -> new RsWithStatus(RsStatus.OK))
        );
    }
}
