/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.conda.meta;

import com.artipie.asto.test.TestResource;
import java.io.IOException;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * Test for {@link InfoIndex.Conda}.
 * @since 0.2
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class InfoIndexCondaTest {

    @Test
    void readsMetadata() throws IOException, JSONException {
        JSONAssert.assertEquals(
            new InfoIndex.Conda(
                new TestResource("7zip-19.00-h59b6b97_2.conda").asInputStream()
            ).json().toString(),
            String.join(
                "\n",
                "{\n",
                "  \"arch\": \"x86_64\",",
                "  \"build\": \"h59b6b97_2\",",
                "  \"build_number\": 2,",
                "  \"constrains\": [",
                "    \"7za <0.0.0a\"",
                "  ],",
                "  \"depends\": [",
                "    \"vc >=14.1,<15.0a0\",",
                "    \"vs2015_runtime >=14.16.27012,<15.0a0\"",
                "  ],",
                // @checkstyle LineLengthCheck (1 line)
                "  \"license\": \"LGPL-2.1-or-later AND LGPL-2.1-or-later WITH unRAR-restriction\",",
                "  \"name\": \"7zip\",",
                "  \"platform\": \"win\",",
                "  \"subdir\": \"win-64\",",
                "  \"timestamp\": 1619516322562,",
                "  \"version\": \"19.00\"",
                "}"
            ),
            true
        );
    }

}
