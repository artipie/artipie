/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm;

import com.artipie.asto.Concatenation;
import com.artipie.asto.Content;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import javax.json.Json;
import javax.json.JsonObject;
import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests tarballs processing.
 * @since 0.6
 */
public class TarballsTest {
    /**
     * Do actual tests with processing data.
     * @param prefix Tarball prefix
     * @param expected Expected absolute tarball link
     * @throws IOException
         */
    @ParameterizedTest
    @CsvSource({
        "http://example.com/, http://example.com/@hello/simple-npm-project/-/@hello/simple-npm-project-1.0.1.tgz",
        "http://example.com/context/path, http://example.com/context/path/@hello/simple-npm-project/-/@hello/simple-npm-project-1.0.1.tgz"
    })
    public void tarballsProcessingWorks(final String prefix, final String expected)
        throws IOException {
        final byte[] data = IOUtils.resourceToByteArray(
            "/storage/@hello/simple-npm-project/meta.json"
        );
        final Tarballs tarballs = new Tarballs(
            new Content.From(data),
            URI.create(prefix).toURL()
        );
        final Content modified = tarballs.value();
        final JsonObject json = new Concatenation(modified)
            .single()
            .map(ByteBuffer::array)
            .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
            .map(StringReader::new)
            .map(reader -> Json.createReader(reader).readObject())
            .blockingGet();
        MatcherAssert.assertThat(
            json.getJsonObject("versions").getJsonObject("1.0.1")
                .getJsonObject("dist").getString("tarball"),
            new IsEqual<>(expected)
        );
    }
}
