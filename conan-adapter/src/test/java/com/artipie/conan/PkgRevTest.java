/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
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
