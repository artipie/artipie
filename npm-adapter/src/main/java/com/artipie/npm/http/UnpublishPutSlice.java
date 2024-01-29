/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm.http;

import com.artipie.ArtipieException;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.StandardRs;
import com.artipie.npm.PackageNameFromUrl;
import com.artipie.npm.misc.DateTimeNowStr;
import com.artipie.npm.misc.DescSortedVersions;
import com.artipie.npm.misc.JsonFromPublisher;
import com.artipie.scheduling.ArtifactEvent;
import com.google.common.collect.Sets;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Pattern;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonPatchBuilder;
import org.reactivestreams.Publisher;

/**
 * Slice to handle `npm unpublish package@0.0.0` command requests.
 * It unpublishes a single version of package when multiple
 * versions are published.
 * @since 0.9
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class UnpublishPutSlice implements Slice {
    /**
     * Pattern for `referer` header value.
     */
    public static final Pattern HEADER = Pattern.compile("unpublish.*");

    /**
     * Abstract Storage.
     */
    private final Storage asto;

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
     * @param storage Abstract storage
     * @param events Events queue
     * @param rname Repository name
     */
    UnpublishPutSlice(final Storage storage, final Optional<Queue<ArtifactEvent>> events,
        final String rname) {
        this.asto = storage;
        this.events = events;
        this.rname = rname;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> publisher
    ) {
        final String pkg = new PackageNameFromUrl(
            line.replaceFirst("/-rev/[^\\s]+", "")
        ).value();
        final Key key = new Key.From(pkg, "meta.json");
        return new AsyncResponse(
            this.asto.exists(key).thenCompose(
                exists -> {
                    final CompletionStage<Response> res;
                    if (exists) {
                        res = new JsonFromPublisher(publisher).json()
                            .thenCompose(update -> this.updateMeta(update, key))
                            .thenAccept(
                                ver -> this.events.ifPresent(
                                    queue -> queue.add(
                                        new ArtifactEvent(
                                            UploadSlice.REPO_TYPE, this.rname, pkg, ver
                                        )
                                    )
                                )
                            ).thenApply(nothing -> StandardRs.OK);
                    } else {
                        res = CompletableFuture.completedFuture(StandardRs.NOT_FOUND);
                    }
                    return res;
                }
            )
        );
    }

    /**
     * Compare two meta files and remove from the meta file of storage info about
     * version that does not exist in another meta file.
     * @param update Meta json file (usually this file is received from body)
     * @param meta Meta json key in storage
     * @return Removed version
     */
    private CompletionStage<String> updateMeta(
        final JsonObject update, final Key meta
    ) {
        return this.asto.value(meta)
            .thenApply(JsonFromPublisher::new)
            .thenCompose(JsonFromPublisher::json).thenCompose(
                source -> {
                    final JsonPatchBuilder patch = Json.createPatchBuilder();
                    final String diff = versionToRemove(update, source);
                    patch.remove(String.format("/versions/%s", diff));
                    patch.remove(String.format("/time/%s", diff));
                    if (source.getJsonObject("dist-tags").containsKey(diff)) {
                        patch.remove(String.format("/dist-tags/%s", diff));
                    }
                    final String latest = new DescSortedVersions(
                        update.getJsonObject("versions")
                    ).value().get(0);
                    patch.add("/dist-tags/latest", latest);
                    patch.add("/time/modified", new DateTimeNowStr().value());
                    return this.asto.save(
                        meta,
                        new Content.From(
                            patch.build().apply(source).toString().getBytes(StandardCharsets.UTF_8)
                        )
                    ).thenApply(nothing -> diff);
                }
            );
    }

    /**
     * Compare two meta files and identify which version does not exist in one of meta files.
     * @param update Meta json file (usually this file is received from body)
     * @param source Meta json from storage
     * @return Version to unpublish.
     */
    private static String versionToRemove(final JsonObject update, final JsonObject source) {
        final String field = "versions";
        final Set<String> diff = Sets.symmetricDifference(
            source.getJsonObject(field).keySet(),
            update.getJsonObject(field).keySet()
        );
        if (diff.size() != 1) {
            throw new ArtipieException(
                String.format(
                    "Failed to unpublish single version. Should be one version, but were `%s`",
                    diff.toString()
                )
            );
        }
        return diff.iterator().next();
    }
}
