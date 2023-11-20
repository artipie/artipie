/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.helm.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Remaining;
import com.artipie.asto.Storage;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.helm.ChartYaml;
import com.artipie.helm.TgzArchive;
import com.artipie.helm.metadata.IndexYaml;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.Login;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rq.RqParams;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import com.artipie.scheduling.ArtifactEvent;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import org.reactivestreams.Publisher;

/**
 * A Slice which accept archived charts, save them into a storage and trigger index.yml reindexing.
 * By default it updates index file after uploading.
 * @since 0.2
 * @todo #13:30min Create an integration test
 *  We need an integration test for this class with described logic of upload from client side
 * @checkstyle MethodBodyCommentsCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class PushChartSlice implements Slice {

    /**
     * Repository type.
     */
    static final String REPO_TYPE = "helm";

    /**
     * The Storage.
     */
    private final Storage storage;

    /**
     * Events queue.
     */
    private final Optional<Queue<ArtifactEvent>> events;

    /**
     * Repository name.
     */
    private final String rname;

    /**
     * Ctor.
     * @param storage The storage.
     * @param events Events queue
     * @param rname Repository name
     */
    PushChartSlice(final Storage storage, final Optional<Queue<ArtifactEvent>> events,
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
        final Optional<String> upd = new RqParams(
            new RequestLineFrom(line).uri()
        ).value("updateIndex");
        return new AsyncResponse(
            memory(body).flatMapCompletable(
                tgz -> new RxStorageWrapper(this.storage).save(
                    new Key.From(tgz.name()),
                    new Content.From(tgz.bytes())
                ).andThen(
                    Completable.defer(
                        () -> {
                            final Completable res;
                            if (!upd.isPresent() || upd.get().equals("true")) {
                                final ChartYaml chart = tgz.chartYaml();
                                res = new IndexYaml(this.storage).update(tgz);
                                this.events.ifPresent(
                                    queue -> queue.add(
                                        new ArtifactEvent(
                                            PushChartSlice.REPO_TYPE, this.rname,
                                            new Login(new Headers.From(headers)).getValue(),
                                            chart.name(), chart.version(), tgz.size()
                                        )
                                    )
                                );
                            } else {
                                res = Completable.complete();
                            }
                            return res;
                        }
                    )
                )
            ).andThen(Single.just(new RsWithStatus(StandardRs.EMPTY, RsStatus.OK)))
        );
    }

    /**
     * Convert buffers into a byte array.
     * @param bufs The list of buffers.
     * @return The byte array.
     */
    static byte[] bufsToByteArr(final List<ByteBuffer> bufs) {
        final Integer size = bufs.stream()
            .map(Buffer::remaining)
            .reduce(Integer::sum)
            .orElse(0);
        final byte[] bytes = new byte[size];
        int pos = 0;
        for (final ByteBuffer buf : bufs) {
            final byte[] tocopy = new Remaining(buf).bytes();
            System.arraycopy(tocopy, 0, bytes, pos, tocopy.length);
            pos += tocopy.length;
        }
        return bytes;
    }

    /**
     * Loads bytes into the memory.
     * @param body The body.
     * @return Bytes in a single byte array
     */
    private static Single<TgzArchive> memory(final Publisher<ByteBuffer> body) {
        return Flowable.fromPublisher(body)
            .toList()
            .map(bufs -> new TgzArchive(bufsToByteArr(bufs)));
    }
}
