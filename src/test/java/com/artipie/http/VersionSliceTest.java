/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.asto.test.TestResource;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link VersionSlice}.
 * @since 0.20
 */
final class VersionSliceTest {
    @Test
    void returnVersionOfApplication() throws IOException {
        final Properties properties = new Properties();
        properties.load(new TestResource(VersionSlice.PROPERTIES_FILE).asInputStream());
        MatcherAssert.assertThat(
            new VersionSlice(),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasBody(
                        String.format(
                            "[{\"version\":\"%s\"}]",
                            properties.getProperty(VersionSlice.VERSION_KEY)
                        ),
                        StandardCharsets.UTF_8
                    )
                ),
                new RequestLine(RqMethod.GET, "/.version")
            )
        );
    }
}
