/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.pypi.http;

import com.artipie.ArtipieException;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Meta;
import com.artipie.asto.Storage;
import com.artipie.asto.streams.ContentAsStream;
import com.artipie.http.Headers;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.ResponseImpl;
import com.artipie.http.Slice;
import com.artipie.http.headers.ContentDisposition;
import com.artipie.http.headers.Login;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.multipart.RqMultipart;
import com.artipie.http.RsStatus;
import com.artipie.http.slice.KeyFromPath;
import com.artipie.pypi.NormalizedProjectName;
import com.artipie.pypi.meta.Metadata;
import com.artipie.pypi.meta.PackageInfo;
import com.artipie.pypi.meta.ValidFilename;
import com.artipie.scheduling.ArtifactEvent;
import com.jcabi.log.Logger;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * WheelSlice save and manage whl and tgz entries.
 */
final class WheelSlice implements Slice {

    private static final String TYPE = "pypi";

    private final Storage storage;

    private final Optional<Queue<ArtifactEvent>> events;

    /**
     * Repository name.
     */
    private final String rname;

    /**
     * Ctor.
     *
     * @param storage Storage.
     * @param events Evenst queue
     * @param rname Repository name
     */
    WheelSlice(final Storage storage, final Optional<Queue<ArtifactEvent>> events,
        final String rname) {
        this.storage = storage;
        this.events = events;
        this.rname = rname;
    }

    @Override
    public CompletableFuture<ResponseImpl> response(
        final RequestLine line,
        final Headers iterable,
        final Content publisher
    ) {
        final Key.From key = new Key.From(UUID.randomUUID().toString());
        return this.filePart(iterable, publisher, key).thenCompose(
            filename -> this.storage.value(key).thenCompose(
                val -> new ContentAsStream<PackageInfo>(val).process(
                    input -> new Metadata.FromArchive(input, filename).read()
                )
            ).thenCompose(
                info -> {
                    final CompletionStage<RsStatus> res;
                    if (new ValidFilename(info, filename).valid()) {
                        final Key name = new Key.From(
                            new KeyFromPath(line.uri().toString()),
                            new NormalizedProjectName.Simple(info.name()).value(),
                            filename
                        );
                        CompletionStage<Void> move = this.storage.move(key, name);
                        if (this.events.isPresent()) {
                            move = move.thenCompose(
                                ignored ->
                                    this.putArtifactToQueue(name, info, filename, iterable)
                            );
                        }
                        res = move.thenApply(ignored -> RsStatus.CREATED);
                    } else {
                        res = this.storage.delete(key)
                            .thenApply(nothing -> RsStatus.BAD_REQUEST);
                    }
                    return res.thenApply(s -> ResponseBuilder.from(s).build());
                }
            )
        ).handle(
            (response, throwable) -> {
                if(throwable != null){
                    return ResponseBuilder.badRequest(throwable).build();
                }
                return response;
            }
        ).toCompletableFuture();
    }

    /**
     * File part from multipart body.
     * @param headers Request headers
     * @param body Request body
     * @param temp Temp key to save the part
     * @return Part with the file
     */
    private CompletionStage<String> filePart(final Headers headers,
        final Publisher<ByteBuffer> body, final Key temp) {
        return Flowable.fromPublisher(
            new RqMultipart(headers, body).inspect(
                (part, inspector) -> {
                    if ("content".equals(new ContentDisposition(part.headers()).fieldName())) {
                        inspector.accept(part);
                    } else {
                        inspector.ignore(part);
                    }
                    final CompletableFuture<Void> res = new CompletableFuture<>();
                    res.complete(null);
                    return res;
                }
            )
        ).doOnNext(
            part -> Logger.debug(this, "WS: multipart request body parsed, part %s found", part)
        ).flatMapSingle(
            part -> SingleInterop.fromFuture(
                this.storage.save(temp, new Content.From(part))
                    .thenRun(() -> Logger.debug(this, "WS: content saved to temp file `%s`", temp.string()))
                    .thenApply(nothing -> new ContentDisposition(part.headers()).fileName())
            )
        ).toList().map(
            items -> {
                if (items.isEmpty()) {
                    throw new ArtipieException("content part was not found");
                }
                if (items.size() > 1) {
                    throw new ArtipieException("multiple content parts were found");
                }
                return items.get(0);
            }
        ).to(SingleInterop.get());
    }

    /**
     * Put uploaded artifact info into events queue.
     * @param key Artifact key in the storage
     * @param info Artifact info
     * @param filename Artifact filename
     * @param headers Request headers
     * @return Completion action
     */
    private CompletionStage<Void> putArtifactToQueue(
        final Key key, final PackageInfo info, final String filename,
        Headers headers
    ) {
        return this.storage.metadata(key).thenApply(meta -> meta.read(Meta.OP_SIZE).get())
            .thenAccept(
                size -> this.events.get().add(
                    new ArtifactEvent(
                        WheelSlice.TYPE, this.rname,
                        new Login(headers).getValue(),
                        String.join("/", info.name(), filename),
                        info.version(), size
                    )
                )
            );
    }
}
