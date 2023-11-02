/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.docker.asto;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.Catalog;
import com.artipie.docker.RepoName;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link AstoDocker}.
 * @since 0.1
 */
final class AstoDockerTest {
    @Test
    void createsAstoRepo() {
        MatcherAssert.assertThat(
            new AstoDocker(new InMemoryStorage()).repo(new RepoName.Simple("repo1")),
            Matchers.instanceOf(AstoRepo.class)
        );
    }

    @Test
    void shouldReadCatalogs() {
        final Storage storage = new InMemoryStorage();
        storage.save(
            new Key.From("repositories/my-alpine/something"),
            new Content.From("1".getBytes())
        ).toCompletableFuture().join();
        storage.save(
            new Key.From("repositories/test/foo/bar"),
            new Content.From("2".getBytes())
        ).toCompletableFuture().join();
        final Catalog catalog = new AstoDocker(storage)
            .catalog(Optional.empty(), Integer.MAX_VALUE)
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            new PublisherAs(catalog.json()).asciiString().toCompletableFuture().join(),
            new IsEqual<>("{\"repositories\":[\"my-alpine\",\"test\"]}")
        );
    }
}
