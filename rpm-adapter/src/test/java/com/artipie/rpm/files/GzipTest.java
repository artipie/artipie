/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm.files;

import com.artipie.asto.test.TestResource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test for {@link Gzip}.
 * @since 0.8
 */
class GzipTest {

    @Test
    void unpacks(final @TempDir Path tmp) throws IOException {
        new Gzip(new TestResource("test.tar.gz").asPath()).unpackTar(tmp);
        MatcherAssert.assertThat(
            Files.readAllLines(tmp.resolve("test.txt")).iterator().next(),
            new IsEqual<>("hello world")
        );
    }

    @Test
    void unpacksGz(final @TempDir Path tmp) throws IOException {
        final Path res = tmp.resolve("res.xml");
        new Gzip(new TestResource("repodata/primary.xml.gz.example").asPath()).unpack(res);
        MatcherAssert.assertThat(
            Files.readAllLines(res).toArray(),
            Matchers.arrayContaining(
                Files.readAllLines(
                    new TestResource("repodata/primary.xml.example").asPath()
                ).toArray()
            )
        );
    }

}
