/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.misc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test for {@link JavaResource}.
 * @since 0.22
 */
class JavaResourceTest {

    @Test
    void copiesResource(final @TempDir Path temp) throws IOException {
        final String file = "log4j.properties";
        final Path res = temp.resolve(file);
        new JavaResource(file).copy(res);
        MatcherAssert.assertThat(
            Files.exists(res),
            new IsEqual<>(true)
        );
    }

}
