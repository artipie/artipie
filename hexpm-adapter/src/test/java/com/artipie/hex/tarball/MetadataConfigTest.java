/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.hex.tarball;

import com.artipie.hex.ResourceUtil;
import java.io.IOException;
import java.nio.file.Files;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link MetadataConfig}.
 * @since 0.1
 */
class MetadataConfigTest {
    /**
     * Metadata.config file.
     */
    private static MetadataConfig metadata;

    @BeforeAll
    static void setUp() throws IOException {
        MetadataConfigTest.metadata = new MetadataConfig(
            Files.readAllBytes(new ResourceUtil("metadata/metadata.config").asPath())
        );
    }

    @Test
    void readApp() {
        MatcherAssert.assertThat(
            MetadataConfigTest.metadata.app(),
            new StringContains("decimal")
        );
    }

    @Test
    void readVersion() {
        MatcherAssert.assertThat(
            MetadataConfigTest.metadata.version(),
            new StringContains("2.0.0")
        );
    }
}
