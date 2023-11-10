/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.gem.ruby;

import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.test.TestResource;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test case for {@link RubyGemDependencies}.
 *
 * @since 1.3
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class RubyGemDependencyTest {
    @Test
    void calculatesDependencies(final @TempDir Path tmp) {
        Stream.of("builder-3.2.4.gem", "file-tail-1.2.0.gem").map(TestResource::new)
            .forEach(res -> res.saveTo(new FileStorage(tmp)));
        final RubyGemDependencies deps = new SharedRuntime().apply(RubyGemDependencies::new)
            .toCompletableFuture().join();
        final ByteBuffer res = deps.dependencies(
            new HashSet<>(
                Arrays.asList(
                    tmp.resolve("builder-3.2.4.gem"),
                    tmp.resolve("file-tail-1.2.0.gem")
                )
            )
        );
        MatcherAssert.assertThat(res.limit(), Matchers.greaterThan(0));
    }
}
