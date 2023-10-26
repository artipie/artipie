/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021-2023 Artipie
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
package  com.artipie.conan;

import java.time.Instant;
import javax.json.JsonObject;
import javax.json.JsonValue;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

/**
 * Tests for PkgRev class.
 * @since 0.1
 */
class PkgRevTest {

    /**
     * Revision json field.
     */
    private static final String REVISION = "revision";

    /**
     * Timestamp json field. Uses ISO 8601 format.
     */
    private static final String TIMESTAMP = "time";

    @Test
    public void pkgRevGeneration() {
        final int revision = 1;
        final PkgRev pkgrev = new PkgRev(revision);
        final JsonObject obj = pkgrev.toJson();
        MatcherAssert.assertThat(
            "The REVISION json field is incorrent",
            PkgRevTest.getJsonStr(obj, PkgRevTest.REVISION).equals(Integer.toString(revision))
        );
        final String timestamp = PkgRevTest.getJsonStr(obj, PkgRevTest.TIMESTAMP);
        MatcherAssert.assertThat(
            "The TIMESTAMP json field is empty",
            !timestamp.isEmpty()
        );
        MatcherAssert.assertThat(
            "The TIMESTAMP json field is incorrent",
            Instant.parse(timestamp).getEpochSecond() <= Instant.now().getEpochSecond()
        );
    }

    private static String getJsonStr(final JsonValue object, final String key) {
        return object.asJsonObject().get(key).toString().replaceAll("\"", "");
    }
}
