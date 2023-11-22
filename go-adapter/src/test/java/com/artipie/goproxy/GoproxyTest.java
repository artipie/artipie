/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.goproxy;

import com.artipie.asto.Content;
import com.artipie.asto.Remaining;
import io.reactivex.Single;
import java.nio.ByteBuffer;
import java.time.Instant;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Unit test for Goproxy class.
 *
 * @since 0.3
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public class GoproxyTest {
    @Test
    public void generatesVersionedJson() {
        final Instant timestamp = Instant.parse("2020-03-17T08:05:12.32496732Z");
        final Single<Content> content = Goproxy.generateVersionedJson(
            "0.0.1", timestamp
        );
        final ByteBuffer data = content.flatMap(Goproxy::readCompletely).blockingGet();
        MatcherAssert.assertThat(
            "Content does not match",
            "{\"Version\":\"v0.0.1\",\"Time\":\"2020-03-17T08:05:12Z\"}",
            Matchers.equalTo(new String(new Remaining(data).bytes()))
        );
    }
}
