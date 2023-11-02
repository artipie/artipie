/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm.asto;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

/**
 * Test for {@link AstoArchive}.
 * @since 1.9
 */
class AstoArchiveTest {

    @Test
    void gzipsItem() throws IOException {
        final Storage asto = new InMemoryStorage();
        final Key.From key = new Key.From("test");
        final String val = "some text";
        asto.save(key, new Content.From(val.getBytes())).join();
        new AstoArchive(asto).gzip(key).toCompletableFuture().join();
        MatcherAssert.assertThat(
            IOUtils.readLines(
                new GZIPInputStream(new ByteArrayInputStream(new BlockingStorage(asto).value(key))),
                StandardCharsets.UTF_8
            ),
            Matchers.contains(val)
        );
    }

}
