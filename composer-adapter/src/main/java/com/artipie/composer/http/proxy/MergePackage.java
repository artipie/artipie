/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer.http.proxy;

import com.artipie.asto.Content;
import com.artipie.composer.JsonPackage;
import com.artipie.composer.misc.ContentAsJson;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

/**
 * Merging info about different versions of packages.
 * @since 0.4
 */
public interface MergePackage {
    /**
     * Merges info about package from local packages file with info
     * about package which is obtained from remote package.
     * @param remote Remote data about package. Usually this file is not big because
     *  it contains info about versions for one package.
     * @return Merged data about one package.
     */
    CompletionStage<Optional<Content>> merge(Optional<? extends Content> remote);

    /**
     * Merging local data with data from remote.
     * @since 0.4
     */
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    class WithRemote implements MergePackage {
        /**
         * Package name.
         */
        private final String name;

        /**
         * Data from local `packages.json` file.
         */
        private final Content local;

        /**
         * Ctor.
         * @param name Package name
         * @param local Data from local `packages.json` file
         */
        WithRemote(final String name, final Content local) {
            this.name = name;
            this.local = local;
        }

        @Override
        public CompletionStage<Optional<Content>> merge(
            final Optional<? extends Content> remote
        ) {
            return WithRemote.packagesFrom(this.local)
                .thenApply(this::packageByNameFrom)
                .thenCombine(
                    WithRemote.packagesFromOpt(remote),
                    (lcl, rmt) -> {
                        final JsonObject builded = this.jsonWithMergedContent(lcl, rmt);
                        final Optional<Content> res;
                        if (builded.keySet().isEmpty()) {
                            res = Optional.empty();
                        } else {
                            res = Optional.of(
                                new Content.From(
                                    Json.createObjectBuilder().add(
                                        "packages", Json.createObjectBuilder().add(
                                            this.name, builded
                                        ).build()
                                    ).build()
                                    .toString()
                                    .getBytes(StandardCharsets.UTF_8)
                                )
                            );
                        }
                        return res;
                    }
                );
        }

        /**
         * Obtains `packages` entry from file.
         * @param pkgs Content of `package.json` file
         * @return Packages entry from file.
         */
        private static CompletionStage<Optional<JsonObject>> packagesFrom(final Content pkgs) {
            return new ContentAsJson(pkgs).value()
                .thenApply(json -> json.getJsonObject("packages"))
                .thenApply(Optional::ofNullable);
        }

        /**
         * Obtains `packages` entry from file.
         * @param pkgs Optional content of `package.json` file
         * @return Packages entry from file if content is presented, otherwise empty..
         */
        private static CompletionStage<Optional<JsonObject>> packagesFromOpt(
            final Optional<? extends Content> pkgs
        ) {
            final CompletionStage<Optional<JsonObject>> res;
            if (pkgs.isPresent()) {
                res = WithRemote.packagesFrom(pkgs.get());
            } else {
                res = CompletableFuture.completedFuture(Optional.empty());
            }
            return res;
        }

        /**
         * Obtains info about one package.
         * @param json Json object for `packages` entry
         * @return Info about one package. If passed json does not
         *  contain package, empty json will be returned.
         */
        private JsonObject packageByNameFrom(final Optional<JsonObject> json) {
            final JsonObject res;
            if (json.isPresent() && json.get().containsKey(this.name)) {
                res = json.get().getJsonObject(this.name);
            } else {
                res = Json.createObjectBuilder().build();
            }
            return res;
        }

        /**
         * Merges info about package from local index with info from remote one.
         * @param lcl Local index file
         * @param rmt Remote index file
         * @return Merged JSON.
         * @checkstyle NestedIfDepthCheck (40 lines)
         */
        private JsonObject jsonWithMergedContent(
            final JsonObject lcl, final Optional<JsonObject> rmt
        ) {
            final Set<String> vrsns = lcl.keySet();
            final JsonObjectBuilder bldr = Json.createObjectBuilder();
            vrsns.forEach(
                vers -> bldr.add(
                    vers, Json.createObjectBuilder(lcl.getJsonObject(vers))
                        .add("uid", UUID.randomUUID().toString())
                        .build()
                )
            );
            if (rmt.isPresent() && rmt.get().containsKey(this.name)) {
                rmt.get().getJsonArray(this.name).stream()
                    .map(JsonValue::asJsonObject)
                    .forEach(
                        entry -> {
                            final String vers = entry.getString(JsonPackage.VRSN);
                            if (!vrsns.contains(vers)) {
                                final JsonObjectBuilder rmtblbdr;
                                rmtblbdr = Json.createObjectBuilder(entry);
                                if (!entry.containsKey("name")) {
                                    rmtblbdr.add("name", this.name);
                                }
                                rmtblbdr.add("uid", UUID.randomUUID().toString());
                                bldr.add(vers, rmtblbdr.build());
                            }
                        }
                );
            }
            return bldr.build();
        }
    }
}
