/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.streams;

import com.artipie.asto.Content;
import com.artipie.asto.ext.ContentAs;
import io.reactivex.Single;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StorageValuePipeline.PublishingOutputStream}.
 *
 * @since 1.12
 */
public final class PublishingOutputStreamTest {
    @Test
    void shouldPublishContentWhenDataIsWroteToOutputStream() throws Exception {
        final Content content;
        try (StorageValuePipeline.PublishingOutputStream output =
            new StorageValuePipeline.PublishingOutputStream()) {
            content = new Content.From(output.publisher());
            output.write("test data".getBytes(StandardCharsets.UTF_8));
            output.write(" test data 2".getBytes(StandardCharsets.UTF_8));
        }
        MatcherAssert.assertThat(
            ContentAs.STRING.apply(Single.just(content)).toFuture().get(),
            new IsEqual<>("test data test data 2")
        );
    }
}
