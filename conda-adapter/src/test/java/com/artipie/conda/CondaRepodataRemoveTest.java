/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.conda;

import com.artipie.asto.test.TestResource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.cactoos.set.SetOf;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * Test for {@link CondaRepodata.Remove}.
 * @since 0.1
 */
class CondaRepodataRemoveTest {

    @Test
    void removesPackagesInfo() throws IOException, JSONException {
        try (InputStream input = new TestResource("repodata.json").asInputStream()) {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            new CondaRepodata.Remove(input, out).perform(
                new SetOf<>(
                    "4b36cb59651f6218449bd71a7d37182f062f545240b502eebed319f77fa54b08",
                    "b37f144a5c2349b1c58ef17a663cb79086a1f2f49e35503e4f411f6f698cee1a",
                    "be2a62bd5a3a6abda7f2309f4f2ddce7bededb40adb91341f18438b246d7fc7e",
                    "47d6dd01a1cff52af31804bbfffb4341fd8676c75d00d120cc66d9709e78ea7f"
                )
            );
            JSONAssert.assertEquals(
                out.toString(),
                String.join(
                    "",
                    "{",
                    "\"packages\":{",
                    "\"decorator-4.2.1-py27_0.tar.bz2\":{",
                    "\"build\":\"py27_0\",",
                    "\"build_number\":0,",
                    "\"depends\":[\"python >=2.7,<2.8.0a0\"],",
                    "\"license\":\"BSD 3-Clause\",",
                    "\"md5\":\"0ebe0cb0d62eae6cd237444ba8fded66\",",
                    "\"name\":\"decorator\",",
                    // @checkstyle LineLengthCheck (1 line)
                    "\"sha256\":\"b5f77880181b37fb2e180766869da6242648aaec5bdd6de89296d9dacd764c14\",",
                    "\"size\":15638,",
                    "\"subdir\":\"linux-64\",",
                    "\"timestamp\":1516376429792,",
                    "\"version\":\"4.2.1\"",
                    "}}",
                    ",\"packages.conda\":{}}"
                ),
                true
            );
        }
    }

    @Test
    void doesNothingIfGivenFileIsEmpty() throws JSONException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final String file = "{\"packages\":{}}";
        new CondaRepodata.Remove(
            new ByteArrayInputStream(file.getBytes()), out
        ).perform(new SetOf<>("abc123", "xyx098"));
        JSONAssert.assertEquals(
            out.toString(), file, true
        );
    }

}
