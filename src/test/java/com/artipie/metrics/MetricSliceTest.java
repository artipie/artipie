/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.metrics;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.AnyOf;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link MetricSlice}.
 *
 * @since 0.10
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class MetricSliceTest {
    @Test
    void shouldReturnMetricsInJsonArray() {
        final String keyone = "one";
        final String keytwo = "two";
        final String json = "[{\"key\":\"%s\",\"value\":%s},{\"key\":\"%s\",\"value\":%s}]";
        final long valone = 1;
        final long valtwo = 2;
        final String dirorder = String.format(json, keyone, valone, keytwo, valtwo);
        final String revorder = String.format(json, keytwo, valtwo, keyone, valone);
        final Storage storage = new InMemoryStorage();
        storage.save(new Key.From(keyone), this.getContent(valone));
        storage.save(new Key.From(keytwo), this.getContent(valtwo));
        MatcherAssert.assertThat(
            new MetricSlice(storage),
            new SliceHasResponse(
                new AllOf<>(
                    Arrays.asList(
                        new RsHasStatus(RsStatus.OK),
                        new AnyOf<>(
                            Arrays.asList(
                                new RsHasBody(dirorder, StandardCharsets.UTF_8),
                                new RsHasBody(revorder, StandardCharsets.UTF_8)
                            )
                        )
                    )
                ),
                new RequestLine(RqMethod.GET, "/api/repositories/")
            )
        );
    }

    /**
     * Get content from the number.
     *
     * @param number The number to get content
     * @return Content.
     */
    private Content getContent(final long number) {
        return new Content.From(String.valueOf(number).getBytes(StandardCharsets.UTF_8));
    }
}
