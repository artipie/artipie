/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.conda.meta;

import com.artipie.asto.test.TestResource;
import java.io.IOException;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * Test for {@link InfoIndex.TarBz}.
 * @since 0.2
 */
class InfoIndexTarBzTest {

    @Test
    void readsMetadata() throws IOException, JSONException {
        JSONAssert.assertEquals(
            new InfoIndex.TarBz(
                new TestResource("anaconda-navigator-1.8.4-py35_0.tar.bz2").asInputStream()
            ).json().toString(),
            String.join(
                "\n",
                "{\n",
                "  \"arch\": \"x86_64\",",
                "  \"build\": \"py35_0\",",
                "  \"build_number\": 0,",
                "  \"depends\": [",
                "    \"anaconda-client\",",
                "    \"anaconda-project\",",
                "    \"chardet\",",
                "    \"pillow\",",
                "    \"psutil\",",
                "    \"pyqt\",",
                "    \"python >=3.5,<3.6.0a0\",",
                "    \"pyyaml\",",
                "    \"qtpy\",",
                "    \"requests\",",
                "    \"setuptools\"",
                "  ],",
                "  \"license\": \"proprietary - Continuum Analytics, Inc.\",",
                "  \"license_family\": \"Proprietary\",",
                "  \"name\": \"anaconda-navigator\",",
                "  \"platform\": \"linux\",",
                "  \"subdir\": \"linux-64\",",
                "  \"timestamp\": 1524671586445,",
                "  \"version\": \"1.8.4\"",
                "}"
            ),
            true
        );
    }

}
