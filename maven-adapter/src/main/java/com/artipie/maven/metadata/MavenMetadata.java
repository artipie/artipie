/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.maven.metadata;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.xembly.Directive;
import org.xembly.Directives;
import org.xembly.Xembler;

/**
 * Maven metadata generator.
 * @since 0.3
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class MavenMetadata {

    /**
     * Current Xembler state.
     */
    private final Directives dirs;

    /**
     * Ctor.
     * @param source Source xembler directives
     */
    public MavenMetadata(final Iterable<Directive> source) {
        this.dirs = new Directives(source);
    }

    /**
     * Update versions.
     * @param items Version names
     * @return Updated metadata
     */
    public MavenMetadata versions(final Set<String> items) {
        final Directives copy = new Directives(this.dirs);
        copy.xpath("/metadata")
            .push().xpath("versioning").remove().pop()
            .xpath("/metadata")
            .add("versioning");
        items.stream().max(Comparator.comparing(Version::new))
            .ifPresent(latest -> copy.add("latest").set(latest).up());
        items.stream().filter(version -> !version.endsWith("SNAPSHOT"))
            .max(Comparator.comparing(Version::new))
            .ifPresent(latest -> copy.add("release").set(latest).up());
        copy.add("versions");
        items.forEach(version -> copy.add("version").set(version).up());
        copy.up();
        copy.addIf("lastUpdated").set(Instant.now().toEpochMilli()).up();
        copy.up();
        return new MavenMetadata(copy);
    }

    /**
     * Save metadata to storage.
     * @param storage Storage to save
     * @param base Base key where to save
     * @return Completion action with key for saved maven-metadata
     */
    public CompletionStage<Key> save(final Storage storage, final Key base) {
        final Key res = new Key.From(base, "maven-metadata.xml");
        return CompletableFuture.supplyAsync(
            () -> new Xembler(this.dirs).xmlQuietly().getBytes(StandardCharsets.UTF_8)
        )
            .thenCompose(data -> storage.save(res, new Content.From(data)))
            .thenApply(nothing -> res);
    }
}
