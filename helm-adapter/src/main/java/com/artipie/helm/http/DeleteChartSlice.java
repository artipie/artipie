/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.helm.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.helm.ChartYaml;
import com.artipie.helm.TgzArchive;
import com.artipie.helm.metadata.IndexYaml;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rs.BaseResponse;
import com.artipie.http.rs.StandardRs;
import com.artipie.scheduling.ArtifactEvent;
import io.reactivex.Single;

import java.net.URI;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Endpoint for removing chart by name or by name and version.
 */
final class DeleteChartSlice implements Slice {
    /**
     * Pattern for endpoint.
     */
    static final Pattern PTRN_DEL_CHART = Pattern.compile(
        "^/charts/(?<name>[a-zA-Z\\-\\d.]+)/?(?<version>[a-zA-Z\\-\\d.]*)$"
    );

    private final Storage storage;

    /**
     * Events queue.
     */
    private final Optional<Queue<ArtifactEvent>> events;

    /**
     * Repository name.
     */
    private final String repoName;

    /**
     * @param storage The storage.
     * @param events Events queue
     * @param repoName Repository name
     */
    DeleteChartSlice(Storage storage, Optional<Queue<ArtifactEvent>> events, String repoName) {
        this.storage = storage;
        this.events = events;
        this.repoName = repoName;
    }

    @Override
    public Response response(RequestLine line, Headers headers, Content body) {
        final URI uri = line.uri();
        final Matcher matcher = DeleteChartSlice.PTRN_DEL_CHART.matcher(uri.getPath());
        if (matcher.matches()) {
            final String chart = matcher.group("name");
            final String vers = matcher.group("version");
            if (vers.isEmpty()) {
                return new AsyncResponse(
                    new IndexYaml(this.storage)
                        .deleteByName(chart)
                        .andThen(this.deleteArchives(chart, Optional.empty()))
                );
            }
            return new AsyncResponse(
                new IndexYaml(this.storage)
                    .deleteByNameAndVersion(chart, vers)
                    .andThen(this.deleteArchives(chart, Optional.of(vers)))
            );
        }
        return BaseResponse.badRequest();
    }

    /**
     * Delete archives from storage which contain chart with specified name and version.
     * @param name Name of chart.
     * @param vers Version of chart. If it is empty, all versions will be deleted.
     * @return OK - archives were successfully removed, NOT_FOUND - in case of absence.
     */
    private Single<Response> deleteArchives(final String name, final Optional<String> vers) {
        final AtomicBoolean wasdeleted = new AtomicBoolean();
        return Single.fromFuture(
            this.storage.list(Key.ROOT)
                .thenApply(
                    keys -> keys.stream()
                        .filter(key -> key.string().endsWith(".tgz"))
                        .collect(Collectors.toList())
                )
                .thenCompose(
                    keys -> CompletableFuture.allOf(
                        keys.stream().map(
                            key -> this.storage.value(key)
                                .thenCompose(Content::asBytesFuture)
                                .thenCompose(bytes -> {
                                    TgzArchive tgz = new TgzArchive(bytes);
                                    final ChartYaml chart = tgz.chartYaml();
                                    if (chart.name().equals(name)) {
                                        return this.wasChartDeleted(chart, vers, key)
                                            .thenCompose(
                                                wasdel -> {
                                                    wasdeleted.compareAndSet(false, wasdel);
                                                    return CompletableFuture.allOf();
                                                }
                                            );
                                    }
                                    return CompletableFuture.allOf();
                                })
                        ).toArray(CompletableFuture[]::new)
                    ).thenApply(
                        noth -> {
                            if (wasdeleted.get()) {
                                this.events.ifPresent(
                                    queue -> queue.add(
                                        vers.map(
                                            item -> new ArtifactEvent(
                                                PushChartSlice.REPO_TYPE, this.repoName, name, item
                                            )
                                        ).orElseGet(
                                            () -> new ArtifactEvent(
                                                PushChartSlice.REPO_TYPE, this.repoName, name
                                            )
                                        )
                                    )
                                );
                                return StandardRs.OK;
                            }
                            return StandardRs.NOT_FOUND;
                        }
                    )
                )
            );
    }

    /**
     * Checks that chart has required version and delete archive from storage in
     * case of existence of the key.
     * @param chart Chart yaml.
     * @param vers Version which should be deleted. If it is empty, all versions should be deleted.
     * @param key Key to archive which will be deleted in case of compliance.
     * @return Was chart by passed key deleted?
     */
    private CompletionStage<Boolean> wasChartDeleted(
        final ChartYaml chart,
        final Optional<String> vers,
        final Key key
    ) {
        final CompletionStage<Boolean> res;
        if (!vers.isPresent() || chart.version().equals(vers.get())) {
            res = this.storage.exists(key).thenCompose(
                exists -> {
                    final CompletionStage<Boolean> result;
                    if (exists) {
                        result = this.storage.delete(key).thenApply(noth -> true);
                    } else {
                        result = CompletableFuture.completedFuture(false);
                    }
                    return result;
                }
            );
        } else {
            res = CompletableFuture.completedFuture(false);
        }
        return res;
    }
}
