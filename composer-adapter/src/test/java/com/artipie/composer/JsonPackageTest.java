/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer;

import com.artipie.asto.Content;
import com.artipie.asto.test.TestResource;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JsonPackage}.
 *
 * @since 0.1
 */
class JsonPackageTest {

    /**
     * Example package read from 'minimal-package.json'.
     */
    private Package pack;

    @BeforeEach
    void init()  {
        this.pack = new JsonPackage(new TestResource("minimal-package.json").asBytes());
    }

    @Test
    void shouldExtractName() {
        MatcherAssert.assertThat(
            this.pack.name()
                .toCompletableFuture().join()
                .key().string(),
            new IsEqual<>("vendor/package.json")
        );
    }

    @Test
    void shouldExtractVersion() {
        MatcherAssert.assertThat(
            this.pack.version(Optional.empty())
                .toCompletableFuture().join()
                .get(),
            new IsEqual<>("1.2.0")
        );
    }
}
