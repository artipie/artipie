/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
        storage.save(new Key.From(keyone), new Content.From(this.getBytes(valone)));
        storage.save(new Key.From(keytwo), new Content.From(this.getBytes(valtwo)));
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
     * Get array of bytes of the string.
     *
     * @param number Number The number to get an array of bytes
     * @return Array of bytes.
     */
    private byte[] getBytes(final long number) {
        return String.valueOf(number).getBytes(StandardCharsets.UTF_8);
    }
}
