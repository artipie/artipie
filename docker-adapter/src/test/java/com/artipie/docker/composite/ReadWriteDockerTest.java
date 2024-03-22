/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.composite;

import com.artipie.asto.Content;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.Catalog;
import com.artipie.docker.RepoName;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.docker.fake.FakeCatalogDocker;
import com.artipie.docker.proxy.ProxyDocker;
import com.artipie.http.BaseResponse;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Test;

import java.util.Optional;

/**
 * Tests for {@link ReadWriteDocker}.
 *
 * @since 0.3
 */
final class ReadWriteDockerTest {

    @Test
    void createsReadWriteRepo() {
        final ReadWriteDocker docker = new ReadWriteDocker(
            new ProxyDocker((line, headers, body) -> BaseResponse.ok()),
            new AstoDocker(new InMemoryStorage())
        );
        MatcherAssert.assertThat(
            docker.repo(new RepoName.Simple("test")),
            new IsInstanceOf(ReadWriteRepo.class)
        );
    }

    @Test
    void delegatesCatalog() {
        final Optional<RepoName> from = Optional.of(new RepoName.Simple("foo"));
        final int limit = 123;
        final Catalog catalog = () -> new Content.From("{...}".getBytes());
        final FakeCatalogDocker fake = new FakeCatalogDocker(catalog);
        final ReadWriteDocker docker = new ReadWriteDocker(
            fake,
            new AstoDocker(new InMemoryStorage())
        );
        final Catalog result = docker.catalog(from, limit).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Forwards from",
            fake.from(),
            new IsEqual<>(from)
        );
        MatcherAssert.assertThat(
            "Forwards limit",
            fake.limit(),
            new IsEqual<>(limit)
        );
        MatcherAssert.assertThat(
            "Returns catalog",
            result,
            new IsEqual<>(catalog)
        );
    }

}
