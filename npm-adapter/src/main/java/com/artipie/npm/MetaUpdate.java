/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.ContentDigest;
import com.artipie.asto.ext.Digests;
import org.apache.commons.codec.binary.Hex;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonPatchBuilder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Updating `meta.json` file.
 * @since 0.9
 */
public interface MetaUpdate {
    /**
     * Update `meta.json` file by the specified prefix.
     * @param prefix The package prefix
     * @param storage Abstract storage
     * @return Completion or error signal.
     */
    CompletableFuture<Void> update(Key prefix, Storage storage);

    /**
     * Update `meta.json` by adding information from the uploaded json.
     * @since 0.9
     */
    class ByJson implements MetaUpdate {
        /**
         * The uploaded json.
         */
        private final JsonObject json;

        /**
         * Ctor.
         * @param json Uploaded json. Usually this file is generated when
         *  command `npm publish` is completed
         */
        public ByJson(final JsonObject json) {
            this.json = json;
        }

        @Override
        public CompletableFuture<Void> update(final Key prefix, final Storage storage) {
            final Key keymeta = new Key.From(prefix, "meta.json");
            return storage.exists(keymeta)
                .thenCompose(
                    exists -> {
                        if (exists) {
                            return storage.value(keymeta)
                                .thenCompose(Content::asJsonObjectFuture)
                                .thenApply(Meta::new);
                        }
                        return CompletableFuture.completedFuture(
                            new Meta(new NpmPublishJsonToMetaSkelethon(this.json).skeleton())
                        );
                    })
                .thenCompose(
                    meta -> {
                        JsonObject updated = meta.updatedMeta(this.json);
                        return storage.save(
                            keymeta, new Content.From(updated.toString().getBytes(StandardCharsets.UTF_8))
                        );
                    }
                );
        }
    }

    /**
     * Update `meta.json` by adding information from the package file
     * from uploaded archive.
     * @since 0.9
     */
    class ByTgz implements MetaUpdate {
        /**
         * Uploaded tgz archive.
         */
        private final TgzArchive tgz;

        /**
         * Ctor.
         * @param tgz Uploaded tgz file
         */
        public ByTgz(final TgzArchive tgz) {
            this.tgz = tgz;
        }

        @Override
        public CompletableFuture<Void> update(final Key prefix, final Storage storage) {
            final String version = "version";
            final JsonPatchBuilder patch = Json.createPatchBuilder();
            patch.add("/dist", Json.createObjectBuilder().build());
            return ByTgz.hash(this.tgz, Digests.SHA512, true)
                .thenAccept(sha -> patch.add("/dist/integrity", String.format("sha512-%s", sha)))
                .thenCombine(
                    ByTgz.hash(this.tgz, Digests.SHA1, false),
                    (nothing, sha) -> patch.add("/dist/shasum", sha)
                ).thenApply(
                    nothing -> {
                        final JsonObject pkg = this.tgz.packageJson();
                        final String name = pkg.getString("name");
                        final String vers = pkg.getString(version);
                        patch.add("/_id", String.format("%s@%s", name, vers));
                        patch.add(
                            "/dist/tarball",
                            String.format("%s/-/%s-%s.tgz", prefix.string(), name, vers)
                        );
                        return patch.build().apply(pkg);
                    }
                )
                .thenApply(
                    json -> {
                        final JsonObject base = new NpmPublishJsonToMetaSkelethon(json).skeleton();
                        final String vers = json.getString(version);
                        final JsonPatchBuilder upd = Json.createPatchBuilder();
                        upd.add("/dist-tags", Json.createObjectBuilder().build());
                        upd.add("/dist-tags/latest", vers);
                        upd.add(String.format("/versions/%s", vers), json);
                        return upd.build().apply(base);
                    }
                )
                .thenCompose(json -> new ByJson(json).update(prefix, storage))
                .toCompletableFuture();
        }

        /**
         * Obtains specified hash value for passed archive.
         * @param tgz Tgz archive
         * @param dgst Digest mode
         * @param encoded Is encoded64?
         * @return Hash value.
         */
        private static CompletionStage<String> hash(
            final TgzArchive tgz, final Digests dgst, final boolean encoded
        ) {
            return new ContentDigest(new Content.From(tgz.bytes()), dgst)
                .bytes()
                .thenApply(
                    bytes -> {
                        final String res;
                        if (encoded) {
                            res = new String(Base64.getEncoder().encode(bytes));
                        } else {
                            res = Hex.encodeHexString(bytes);
                        }
                        return res;
                    }
                );
        }
    }
}
