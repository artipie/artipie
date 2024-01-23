/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.debian.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Meta;
import com.artipie.asto.Storage;
import com.artipie.asto.streams.ContentAsStream;
import com.artipie.debian.Config;
import com.artipie.debian.metadata.Control;
import com.artipie.debian.metadata.ControlField;
import com.artipie.debian.metadata.InRelease;
import com.artipie.debian.metadata.PackagesItem;
import com.artipie.debian.metadata.Release;
import com.artipie.debian.metadata.UniquePackage;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.Login;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.slice.KeyFromPath;
import com.artipie.scheduling.ArtifactEvent;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.reactivestreams.Publisher;

/**
 * Debian update slice adds uploaded slice to the storage and updates Packages index.
 * @since 0.1
 */
public final class UpdateSlice implements Slice {

    /**
     * Repository type name.
     */
    private static final String REPO_TYPE = "debian";

    /**
     * Abstract storage.
     */
    private final Storage asto;

    /**
     * Repository configuration.
     */
    private final Config config;

    /**
     * Artifact events.
     */
    private final Optional<Queue<ArtifactEvent>> events;

    /**
     * Ctor.
     * @param asto Abstract storage
     * @param config Repository configuration
     * @param events Artifact events
     */
    public UpdateSlice(
        final Storage asto, final Config config, final Optional<Queue<ArtifactEvent>> events
    ) {
        this.asto = asto;
        this.config = config;
        this.events = events;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Key key = new KeyFromPath(new RequestLineFrom(line).uri().getPath());
        return new AsyncResponse(
            this.asto.save(key, new Content.From(body))
                .thenCompose(nothing -> this.asto.value(key))
                .thenCompose(
                    content -> new ContentAsStream<String>(content)
                        .process(input -> new Control.FromInputStream(input).asString())
                )
                .thenCompose(
                    control -> {
                        final List<String> common = new ControlField.Architecture().value(control)
                            .stream().filter(item -> this.config.archs().contains(item))
                            .collect(Collectors.toList());
                        final CompletionStage<Response> res;
                        if (common.isEmpty()) {
                            res = this.asto.delete(key).thenApply(
                                nothing -> new RsWithStatus(RsStatus.BAD_REQUEST)
                            );
                        } else {
                            CompletionStage<Void> upd = this.generateIndexes(key, control, common);
                            if (this.events.isPresent()) {
                                upd = upd.thenCompose(
                                    nothing -> this.logEvents(
                                        key, control, common, new Headers.From(headers)
                                    )
                                );
                            }
                            res = upd.thenApply(nothing -> StandardRs.OK);
                        }
                        return res;
                    }
                ).handle(
                    (resp, throwable) -> {
                        final CompletionStage<Response> res;
                        if (throwable == null) {
                            res = CompletableFuture.completedFuture(resp);
                        } else {
                            res = this.asto.delete(key)
                                .thenApply(nothing -> new RsWithStatus(RsStatus.INTERNAL_ERROR));
                        }
                        return res;
                    }
            ).thenCompose(Function.identity())
        );
    }

    /**
     * Generates Packages, Release and InRelease indexes.
     * @param key Deb package key
     * @param control Control file content
     * @param archs Architectures
     * @return Completion action
     */
    private CompletionStage<Void> generateIndexes(final Key key, final String control,
        final List<String> archs) {
        final Release release = new Release.Asto(this.asto, this.config);
        return new PackagesItem.Asto(this.asto).format(control, key).thenCompose(
            item -> CompletableFuture.allOf(
                archs.stream().map(
                    arc -> String.format(
                        "dists/%s/main/binary-%s/Packages.gz",
                        this.config.codename(), arc
                    )
                ).map(
                    index -> new UniquePackage(this.asto)
                        .add(Collections.singletonList(item), new Key.From(index))
                        .thenCompose(nothing -> release.update(new Key.From(index)))
                ).toArray(CompletableFuture[]::new)
            ).thenCompose(
                nothing -> new InRelease.Asto(this.asto, this.config).generate(release.key())
            )
        );
    }

    /**
     * Adds new package data into events queue. As one package can be suitable for several
     * architectures, we add architecture to package name and log package for each architecture.
     * For example:
     * aglfn_all
     * aglfn_amb46
     * aglfn_arm
     * @param artifact Artifact key
     * @param control Control metadata
     * @param archs Supported architectures
     * @param hdrs Request headers
     * @return Completion action
         */
    private CompletionStage<Void> logEvents(
        final Key artifact, final String control, final List<String> archs, final Headers hdrs
    ) {
        return this.asto.metadata(artifact).thenApply(meta -> meta.read(Meta.OP_SIZE).get())
            .thenAccept(
                size -> {
                    final String name = new ControlField.Package().value(control).get(0);
                    final String version = new ControlField.Version().value(control).get(0);
                    final String owner = new Login(hdrs).getValue();
                    archs.forEach(
                        val -> this.events.get().add(
                            new ArtifactEvent(
                                UpdateSlice.REPO_TYPE, this.config.codename(), owner,
                                String.join("_", name, val), version, size
                            )
                        )
                    );
                }
        );
    }
}
