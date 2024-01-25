/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.composer.http.Archive;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import javax.json.Json;
import javax.json.JsonObject;

/**
 * PHP Composer repository that stores packages in a {@link Storage}.
 *
 * @since 0.3
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class AstoRepository implements Repository {

    /**
     * Key to all packages.
     */
    public static final Key ALL_PACKAGES = new AllPackages();

    /**
     * The storage.
     */
    private final Storage asto;

    /**
     * Prefix with url for uploaded archive.
     */
    private final Optional<String> prefix;

    /**
     * Ctor.
     * @param storage Storage to store all repository data.
     */
    public AstoRepository(final Storage storage) {
        this(storage, Optional.empty());
    }

    /**
     * Ctor.
     * @param storage Storage to store all repository data.
     * @param prefix Prefix with url for uploaded archive.
     */
    public AstoRepository(final Storage storage, final Optional<String> prefix) {
        this.asto = storage;
        this.prefix = prefix;
    }

    @Override
    public CompletionStage<Optional<Packages>> packages() {
        return this.packages(AstoRepository.ALL_PACKAGES);
    }

    @Override
    public CompletionStage<Optional<Packages>> packages(final Name name) {
        return this.packages(name.key());
    }

    @Override
    public CompletableFuture<Void> addJson(final Content content, final Optional<String> vers) {
        final Key key = new Key.From(UUID.randomUUID().toString());
        return this.asto.save(key, content).thenCompose(
            nothing -> this.asto.value(key)
                .thenApply(PublisherAs::new)
                .thenCompose(PublisherAs::bytes)
                .thenCompose(
                    bytes -> {
                        final Package pack = new JsonPackage(bytes);
                        return CompletableFuture.allOf(
                            this.packages().thenCompose(
                                packages -> packages.orElse(new JsonPackages())
                                    .add(pack, vers)
                                    .thenCompose(
                                        pkgs -> pkgs.save(
                                            this.asto, AstoRepository.ALL_PACKAGES
                                        )
                                    )
                            ).toCompletableFuture(),
                            pack.name().thenCompose(
                                name -> this.packages(name).thenCompose(
                                    packages -> packages.orElse(new JsonPackages())
                                        .add(pack, vers)
                                        .thenCompose(
                                            pkgs -> pkgs.save(this.asto, name.key())
                                        )
                                )
                            ).toCompletableFuture()
                        ).thenCompose(
                            ignored -> this.asto.delete(key)
                        );
                    }
                )
        );
    }

    @Override
    public CompletableFuture<Void> addArchive(final Archive archive, final Content content) {
        final Key key = archive.name().artifact();
        final Key rand = new Key.From(UUID.randomUUID().toString());
        final Key tmp = new Key.From(rand, archive.name().full());
        return this.asto.save(key, content)
            .thenCompose(
                nothing -> this.asto.value(key)
                    .thenCompose(
                        cont -> archive.composerFrom(cont)
                            .thenApply(
                                compos -> AstoRepository.addVersion(compos, archive.name())
                            ).thenCombine(
                                this.asto.value(key),
                                (compos, cnt) -> archive.replaceComposerWith(
                                    cnt,
                                    compos.toString()
                                        .getBytes(StandardCharsets.UTF_8)
                                ).thenCompose(arch -> this.asto.save(tmp, arch))
                                .thenCompose(noth -> this.asto.delete(key))
                                .thenCompose(noth -> this.asto.move(tmp, key))
                                .thenCombine(
                                    this.packages(),
                                    (noth, packages) -> packages.orElse(new JsonPackages())
                                        .add(
                                            new JsonPackage(this.addDist(compos, key)),
                                            Optional.empty()
                                        )
                                        .thenCompose(
                                            pkgs -> pkgs.save(
                                                this.asto, AstoRepository.ALL_PACKAGES
                                            )
                                        )
                                ).thenCompose(Function.identity())
                            ).thenCompose(Function.identity())
                    )
            );
    }

    @Override
    public CompletableFuture<Content> value(final Key key) {
        return this.asto.value(key);
    }

    @Override
    public Storage storage() {
        return this.asto;
    }

    @Override
    public CompletableFuture<Boolean> exists(final Key key) {
        return this.asto.exists(key);
    }

    @Override
    public CompletableFuture<Void> save(final Key key, final Content content) {
        return this.asto.save(key, content);
    }

    @Override
    public <T> CompletionStage<T> exclusively(
        final Key key,
        final Function<Storage, CompletionStage<T>> operation
    ) {
        return this.asto.exclusively(key, operation);
    }

    @Override
    public CompletableFuture<Void> move(final Key source, final Key destination) {
        return this.asto.move(source, destination);
    }

    @Override
    public CompletableFuture<Void> delete(final Key key) {
        return this.asto.delete(key);
    }

    /**
     * Add version field to composer json.
     * @param compos Composer json file
     * @param name Instance of name for obtaining version
     * @return Composer json with added version.
     */
    private static JsonObject addVersion(final JsonObject compos, final Archive.Name name) {
        return Json.createObjectBuilder(compos)
            .add(JsonPackage.VRSN, name.version())
            .build();
    }

    /**
     * Add `dist` field to composer json.
     * @param compos Composer json file
     * @param path Prefix path for uploading tgz archive
     * @return Composer json with added `dist` field.
     */
    private byte[] addDist(final JsonObject compos, final Key path) {
        final String url = this.prefix.orElseThrow(
            () -> new IllegalStateException("Prefix url for `dist` for uploaded archive was empty.")
        ).replaceAll("/$", "");
        try {
            return Json.createObjectBuilder(compos).add(
                "dist", Json.createObjectBuilder()
                    .add("url", new URI(String.format("%s/%s", url, path.string())).toString())
                    .add("type", "zip")
                    .build()
                ).build()
                .toString()
                .getBytes(StandardCharsets.UTF_8);
        } catch (final URISyntaxException exc) {
            throw new IllegalStateException(
                String.format("Failed to combine url `%s` with path `%s`", url, path.string()),
                exc
            );
        }
    }

    /**
     * Reads packages description from storage.
     *
     * @param key Content location in storage.
     * @return Packages found by name, might be empty.
     */
    private CompletionStage<Optional<Packages>> packages(final Key key) {
        return this.asto.exists(key).thenCompose(
            exists -> {
                final CompletionStage<Optional<Packages>> packages;
                if (exists) {
                    packages = this.asto.value(key)
                        .thenApply(JsonPackages::new)
                        .thenApply(Optional::of);
                } else {
                    packages = CompletableFuture.completedFuture(Optional.empty());
                }
                return packages;
            }
        );
    }
}
