/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.Login;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.RsStatus;
import com.artipie.rpm.RepoConfig;
import com.artipie.rpm.asto.AstoRepoAdd;
import com.artipie.scheduling.ArtifactEvent;
import com.google.common.base.Splitter;
import com.google.common.collect.Streams;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Slice for rpm packages upload.
 */
public final class RpmUpload implements Slice {

    /**
     * Temp key for the packages to remove.
     */
    public static final Key TO_ADD = new Key.From(".add");

    /**
     * Repository type.
     */
    static final String REPO_TYPE = "rpm";

    /**
     * Asto storage.
     */
    private final Storage asto;

    /**
     * Repo config.
     */
    private final RepoConfig config;

    /**
     * Artipie artifact upload/remove events.
     */
    private final Optional<Queue<ArtifactEvent>> events;

    /**
     * RPM repository HTTP API.
     *
     * @param storage Storage
     * @param config Repository configuration
     * @param events Artipie artifact upload/remove events
     */
    RpmUpload(final Storage storage, final RepoConfig config,
        final Optional<Queue<ArtifactEvent>> events) {
        this.asto = storage;
        this.config = config;
        this.events = events;
    }

    @Override
    public Response response(
        final RequestLine line, final Headers headers,
        final Content body) {
        final Request request = new Request(line);
        final Key key = request.file();
        final CompletionStage<Boolean> conflict;
        if (request.override()) {
            conflict = CompletableFuture.completedFuture(false);
        } else {
            conflict = this.asto.exists(key);
        }
        return new AsyncResponse(
            conflict.thenCompose(
                conflicts -> {
                    final CompletionStage<RsStatus> status;
                    if (conflicts) {
                        status = CompletableFuture.completedFuture(RsStatus.CONFLICT);
                    } else {
                        status = this.asto.save(
                            new Key.From(RpmUpload.TO_ADD, key), new Content.From(body)
                        ).thenCompose(
                            ignored -> {
                                final CompletionStage<Void> result;
                                if (request.skipUpdate()
                                    || this.config.mode() == RepoConfig.UpdateMode.CRON) {
                                    result = CompletableFuture.allOf();
                                } else {
                                    final AstoRepoAdd repo =
                                        new AstoRepoAdd(this.asto, this.config);
                                    result = this.events.map(
                                        queue -> repo.performWithResult().thenAccept(
                                            list -> list.forEach(
                                                info -> queue.add(
                                                    new ArtifactEvent(
                                                        RpmUpload.REPO_TYPE, this.config.name(),
                                                        new Login(headers).getValue(),
                                                        info.name(), info.version(),
                                                        info.packageSize()
                                                    )
                                                )
                                            )
                                        )
                                    ).orElseGet(repo::perform);
                                }
                                return result;
                            }
                        ).thenApply(nothing -> RsStatus.ACCEPTED);
                    }
                    return status;
                }
            ).thenApply(s -> ResponseBuilder.from(s).build())
        );
    }

    /**
     * Request line.
     *
     * @since 0.9
     */
    static final class Request {

        /**
         * RegEx pattern for path.
         */
        public static final Pattern PTRN = Pattern.compile("^/(?<rpm>.*\\.rpm)");

        /**
         * Request line.
         */
        private final RequestLine line;

        /**
         * Ctor.
         *
         * @param line Line from request
         */
        Request(final RequestLine line) {
            this.line = line;
        }

        /**
         * Returns file key.
         *
         * @return File key
         */
        public Key file() {
            return new Key.From(this.path().group("rpm"));
        }

        /**
         * Returns override param.
         *
         * @return Override param value, <code>false</code> - if absent
         */
        public boolean override() {
            return this.hasParamValue("override=true");
        }

        /**
         * Returns `skip_update` param.
         *
         * @return Skip update param value, <code>false</code> - if absent
         */
        public boolean skipUpdate() {
            return this.hasParamValue("skip_update=true");
        }

        /**
         * Returns `force` param.
         *
         * @return Force param value, <code>false</code> - if absent
         */
        public boolean force() {
            return this.hasParamValue("force=true");
        }

        /**
         * Matches request path by RegEx pattern.
         *
         * @return Path matcher.
         */
        private Matcher path() {
            final String path = this.line.uri().getPath();
            final Matcher matcher = PTRN.matcher(path);
            if (!matcher.matches()) {
                throw new IllegalStateException(String.format("Unexpected path: %s", path));
            }
            return matcher;
        }

        /**
         * Checks that request query contains param with value.
         *
         * @param param Param with value string.
         * @return Result is <code>true</code> if there is param with value,
         *  <code>false</code> - otherwise.
         */
        private boolean hasParamValue(final String param) {
            return Optional.ofNullable(this.line.uri().getQuery())
                .map(query -> Streams.stream(Splitter.on("&").split(query)))
                .orElse(Stream.empty())
                .anyMatch(part -> part.equals(param));
        }
    }
}
