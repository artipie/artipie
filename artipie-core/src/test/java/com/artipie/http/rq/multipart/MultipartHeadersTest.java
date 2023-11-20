/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.rq.multipart;

import com.artipie.http.headers.ContentDisposition;
import com.artipie.http.headers.Header;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test case for multipart headers.
 * @since 1.0
 * @checkstyle MagicNumberCheck (500 lines)
 * @checkstyle ModifiedControlVariableCheck (500 lines)
 */
final class MultipartHeadersTest {

    @Test
    @SuppressWarnings("PMD.OneDeclarationPerLine")
    void buildHeadersFromChunks() throws Exception {
        final MultipartHeaders headers = new MultipartHeaders(10);
        final String source = String.join(
            "\r\n",
            "Accept: application/json",
            "Content-length: 100",
            "Connection: keep-alive",
            "Content-Disposition: form-data; name=\"content\"; filename=\"My-Test.txt\""
        );
        for (int pos = 0, take = 3; pos < source.length(); pos += take, ++take) {
            if (pos + take > source.length()) {
                take = source.length() - pos;
            }
            final String sub = source.substring(pos, pos + take);
            headers.push(ByteBuffer.wrap(sub.getBytes()));
        }
        MatcherAssert.assertThat(
            headers,
            Matchers.containsInAnyOrder(
                new Header("Accept", "application/json"),
                new Header("Connection", "keep-alive"),
                new Header("Content-length", "100"),
                new Header(
                    "Content-Disposition", "form-data; name=\"content\"; filename=\"My-Test.txt\""
                )
            )
        );
    }

    @Test
    void buildHeadersWithColon() {
        final MultipartHeaders headers = new MultipartHeaders(1);
        headers.push(
            ByteBuffer.wrap(
                "Content-Disposition: form-data; name=\":action\""
                    .getBytes(StandardCharsets.US_ASCII)
            )
        );
        MatcherAssert.assertThat(
            new ContentDisposition(headers).fieldName(),
            new IsEqual<>(":action")
        );
    }
}
