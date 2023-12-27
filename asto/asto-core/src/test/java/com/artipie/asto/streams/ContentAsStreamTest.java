/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.streams;

import com.artipie.asto.Content;
import com.artipie.asto.misc.UncheckedIOFunc;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link ContentAsStream}.
 * @since 1.4
 */
class ContentAsStreamTest {

    @Test
    void processesItem() {
        final Charset charset = StandardCharsets.UTF_8;
        MatcherAssert.assertThat(
            new ContentAsStream<List<String>>(new Content.From("one\ntwo\nthree".getBytes(charset)))
                .process(new UncheckedIOFunc<>(input -> IOUtils.readLines(input, charset)))
                .toCompletableFuture().join(),
            Matchers.contains("one", "two", "three")
        );
    }

}
