/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.docker.asto;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.Catalog;
import com.artipie.docker.misc.Pagination;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link AstoDocker}.
 */
final class AstoDockerTest {
    @Test
    void createsAstoRepo() {
        MatcherAssert.assertThat(
            new AstoDocker("test_registry", new InMemoryStorage()).repo("repo1"),
            Matchers.instanceOf(AstoRepo.class)
        );
    }

    @Test
    void shouldReadCatalogs() {
        final Storage storage = new InMemoryStorage();
        storage.save(
            new Key.From("repositories/my-alpine/something"),
            new Content.From("1".getBytes())
        ).join();
        storage.save(
            new Key.From("repositories/test/foo/bar"),
            new Content.From("2".getBytes())
        ).join();
        final Catalog catalog = new AstoDocker("test_registry", storage)
            .catalog(Pagination.empty())
            .join();
        MatcherAssert.assertThat(
            catalog.json().asString(),
            new IsEqual<>("{\"repositories\":[\"my-alpine\",\"test\"]}")
        );
    }
}
