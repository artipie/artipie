/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.gem;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.SubStorage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import java.util.Arrays;
import java.util.HashSet;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link Gem} SDK.
 *
 * @since 1.0
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class GemTest {

    @Test
    void updateRepoIndex() throws Exception {
        final Storage repo = new InMemoryStorage();
        final Key target = new Key.From("gems", UUID.randomUUID().toString());
        new TestResource("builder-3.2.4.gem").saveTo(repo, target);
        final Gem gem = new Gem(repo);
        gem.update(target).toCompletableFuture().join();
        MatcherAssert.assertThat(
            new BlockingStorage(repo).list(Key.ROOT)
                .stream().map(Key::string)
                .collect(Collectors.toSet()),
            Matchers.hasItems(
                "prerelease_specs.4.8",
                "prerelease_specs.4.8.gz",
                "specs.4.8",
                "specs.4.8.gz",
                "latest_specs.4.8",
                "latest_specs.4.8.gz",
                "quick/Marshal.4.8/builder-3.2.4.gemspec.rz",
                "gems/builder-3.2.4.gem"
            )
        );
    }

    @Test
    void parseGemDependencies() throws Exception {
        final Storage repo = new InMemoryStorage();
        Stream.of("builder-3.2.4.gem", "file-tail-1.2.0.gem")
            .map(TestResource::new)
            .forEach(tr -> tr.saveTo(new SubStorage(new Key.From("gems"), repo)));
        MatcherAssert.assertThat(
            new Gem(repo).dependencies(
                new HashSet<>(Arrays.asList("builder", "file-tail"))
            ).toCompletableFuture().join().limit(),
            Matchers.greaterThan(0)
        );
    }
}
