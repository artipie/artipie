/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.streams;

import com.artipie.asto.Content;
import io.reactivex.Flowable;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StorageValuePipeline.PublishingOutputStream}.
 *
 * @since 1.12
 */
public final class ContentAsInputStreamTest {

    @Test
    void shouldGetContentDataFromInputStream() throws Exception {
        final StorageValuePipeline.ContentAsInputStream cnt =
            new StorageValuePipeline.ContentAsInputStream(
                new Content.From(
                    Flowable.fromArray(
                        ByteBuffer.wrap("test data".getBytes(StandardCharsets.UTF_8)),
                        ByteBuffer.wrap(" test data2".getBytes(StandardCharsets.UTF_8))
                    )
                )
            );
        try (BufferedReader in = new BufferedReader(new InputStreamReader(cnt.inputStream()))) {
            MatcherAssert.assertThat(
                in.readLine(),
                new IsEqual<>("test data test data2")
            );
        }
    }

    @Test
    void shouldEndOfStreamWhenContentIsEmpty() throws Exception {
        final StorageValuePipeline.ContentAsInputStream cnt =
            new StorageValuePipeline.ContentAsInputStream(Content.EMPTY);
        try (InputStream stream = cnt.inputStream()) {
            final byte[] buf = new byte[8];
            MatcherAssert.assertThat(
                stream.read(buf),
                new IsEqual<>(-1)
            );
        }
    }
}
