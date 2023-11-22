/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */

package com.artipie.hex.tarball;

import com.artipie.hex.ResourceUtil;
import java.io.IOException;
import java.nio.file.Files;
import org.apache.commons.compress.utils.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link TarReader}.
 * @since 0.1
 */
class TarReaderTest {
    @Test
    void readHexPackageName() throws IOException {
        final byte[] content = IOUtils.toByteArray(
            Files.newInputStream(new ResourceUtil("tarballs/decimal-2.0.0.tar").asPath())
        );
        MatcherAssert.assertThat(
            new TarReader(content)
                .readEntryContent("metadata.config")
                .isPresent(),
            new IsEqual<>(true)
        );
    }

}
