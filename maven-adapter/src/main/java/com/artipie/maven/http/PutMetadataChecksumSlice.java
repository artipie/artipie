/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Meta;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.ContentDigest;
import com.artipie.asto.ext.Digests;
import com.artipie.asto.ext.KeyLastPart;
import com.artipie.asto.rx.RxStorageWrapper;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.Login;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.maven.Maven;
import com.artipie.maven.ValidUpload;
import com.artipie.scheduling.ArtifactEvent;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.reactivestreams.Publisher;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This slice accepts PUT requests with maven-metadata.xml checksums, picks up corresponding
 * maven-metadata.xml from the package upload temp location and saves the checksum. If upload
 * is ready to be added in the repository (see {@link ValidUpload#ready(Key)}), this slice initiate
 * repository update.
 */
public final class PutMetadataChecksumSlice implements Slice {

    /**
     * Metadata pattern.
     */
    static final Pattern PTN =
        Pattern.compile("^/(?<pkg>.+)/maven-metadata.xml.(?<alg>md5|sha1|sha256|sha512)");

    /**
     * Repository type.
     */
    private static final String REPO_TYPE = "maven";

    /**
     * Response with status BAD_REQUEST.
     */
    private static final Response BAD_REQUEST = new RsWithStatus(RsStatus.BAD_REQUEST);

    /**
     * Abstract storage.
     */
    private final Storage asto;

    /**
     * Upload validation.
     */
    private final ValidUpload valid;

    /**
     * Maven repository.
     */
    private final Maven mvn;

    /**
     * Repository name.
     */
    private final String rname;

    /**
     * Artifact upload events queue.
     */
    private final Optional<Queue<ArtifactEvent>> events;

    /**
     * Ctor.
     *
     * @param asto Abstract storage
     * @param valid Upload validation
     * @param mvn Maven repository
     * @param rname Repository name
     * @param events Events queue
     */
    public PutMetadataChecksumSlice(final Storage asto, final ValidUpload valid, final Maven mvn,
        final String rname, final Optional<Queue<ArtifactEvent>> events) {
        this.asto = asto;
        this.valid = valid;
        this.mvn = mvn;
        this.rname = rname;
        this.events = events;
    }

    @Override
    public Response response(final RequestLine line, final Iterable<Map.Entry<String, String>> headers,
                             final Publisher<ByteBuffer> body) {
        final Matcher matcher = PutMetadataChecksumSlice.PTN.matcher(line.uri().getPath());
        if (matcher.matches()) {
            final String alg = matcher.group("alg");
            final String pkg = matcher.group("pkg");
            return new AsyncResponse(
                this.findAndSave(body, alg, pkg).thenCompose(
                    key -> {
                        final CompletionStage<Response> resp;
                        if (key.isPresent() && key.get().parent().isPresent()
                            && key.get().parent().get().parent().isPresent()) {
                            final Key location = key.get().parent().get().parent().get();
                            resp = this.valid.ready(location).thenCompose(
                                ready -> {
                                    final CompletionStage<Response> action;
                                    if (ready) {
                                        action = this.validateAndUpdate(pkg, location, headers);
                                    } else {
                                        action = CompletableFuture.completedFuture(
                                            new RsWithStatus(RsStatus.CREATED)
                                        );
                                    }
                                    return action;
                                }
                            );
                        } else {
                            resp = CompletableFuture.completedFuture(
                                PutMetadataChecksumSlice.BAD_REQUEST
                            );
                        }
                        return resp;
                    }
                )
            );
        }
        return new RsWithStatus(RsStatus.BAD_REQUEST);
    }

    /**
     * Validates and, if valid, starts update process.
     * @param pkg Package name
     * @param location Temp upload location
     * @param headers Request headers
     * @return Response: BAD_REQUEST if not valid, CREATED otherwise
     */
    private CompletionStage<Response> validateAndUpdate(final String pkg, final Key location,
        final Iterable<Map.Entry<String, String>> headers) {
        return this.valid.validate(location, new Key.From(pkg)).thenCompose(
            correct -> {
                final CompletionStage<Response> upd;
                if (correct) {
                    CompletionStage<Void> res = this.mvn.update(location, new Key.From(pkg));
                    if (this.events.isPresent()) {
                        final String version = new KeyLastPart(location).get();
                        res = res.thenCompose(
                            ignored -> this.artifactSize(new Key.From(pkg, version))
                        ).thenAccept(
                            size -> this.events.get().add(
                                new ArtifactEvent(
                                    PutMetadataChecksumSlice.REPO_TYPE, this.rname,
                                    new Login(new Headers.From(headers)).getValue(),
                                    MavenSlice.EVENT_INFO.formatArtifactName(pkg), version, size
                                )
                            )
                        );
                    }
                    upd = res.thenApply(ignored -> new RsWithStatus(RsStatus.CREATED));
                } else {
                    upd = CompletableFuture.completedFuture(PutMetadataChecksumSlice.BAD_REQUEST);
                }
                return upd;
            }
        );
    }

    /**
     * Searcher for the suitable maven-metadata.xml and saves checksum to the correct location,
     * returns suitable maven-metadata.xml key.
     * @param body Request body
     * @param alg Algorithm
     * @param pkg Package name
     * @return Completion action
     */
    private CompletionStage<Optional<Key>> findAndSave(final Publisher<ByteBuffer> body,
        final String alg, final String pkg) {
        return new Content.From(body).asStringFuture().thenCompose(
            sum -> new RxStorageWrapper(this.asto).list(
                new Key.From(UploadSlice.TEMP, pkg)
            ).flatMapObservable(Observable::fromIterable)
                .filter(item -> item.string().endsWith("maven-metadata.xml"))
                .flatMapSingle(
                    item -> Single.fromFuture(
                        this.asto.value(item).thenCompose(
                            pub -> new ContentDigest(
                                pub, Digests.valueOf(alg.toUpperCase(Locale.US))
                            ).hex()
                        ).thenApply(hex -> new ImmutablePair<>(item, hex))
                    )
                ).filter(pair -> pair.getValue().equals(sum))
                .singleOrError()
                .flatMap(
                    pair -> SingleInterop.fromFuture(
                        this.asto.save(
                            new Key.From(
                                String.format(
                                    "%s.%s", pair.getKey().string(), alg
                                )
                            ),
                            new Content.From(
                                sum.getBytes(StandardCharsets.US_ASCII)
                            )
                        ).thenApply(
                            nothing -> Optional.of(pair.getKey())
                        )
                    )
                )
                .onErrorReturn(ignored -> Optional.empty())
                .to(SingleInterop.get())
        );
    }

    /**
     * Calculate artifacts size.
     * @param location Arttifacts location
     * @return Completable action with size
     */
    private CompletionStage<Long> artifactSize(final Key location) {
        return this.asto.list(location).thenApply(
            MavenSlice.EVENT_INFO::artifactPackage
        ).thenCompose(this.asto::metadata).thenApply(meta -> meta.read(Meta.OP_SIZE).get());
    }
}
