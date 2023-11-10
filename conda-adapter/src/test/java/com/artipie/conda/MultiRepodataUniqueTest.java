/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.conda;

import com.artipie.asto.test.TestResource;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import org.cactoos.list.ListOf;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * Test for {@link MultiRepodata.Unique}.
 * @since 0.3
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class MultiRepodataUniqueTest {

    @ParameterizedTest
    @CsvSource({
        "m2p_first_1.json,m2p_second_1.json,m2p_res_1.json",
        "m2p_first_2.json,m2p_second_1.json,m2p_res_2.json",
        "m2p_first_1.json,m2p_second_3.json,m2p_res_3.json",
        "m2p_first_4.json,m2p_second_4.json,m2p_res_4.json",
        "m2p_first_5.json,m2p_second_1.json,m2p_second_1.json",
        "m2p_second_1.json,m2p_first_5.json,m2p_second_1.json"
    })
    void mergesTwoPackages(final String first, final String second, final String res)
        throws UnsupportedEncodingException, JSONException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        new MultiRepodata.Unique().merge(
            new ListOf<InputStream>(this.resourceStream(first), this.resourceStream(second)), out
        );
        JSONAssert.assertEquals(
            out.toString(StandardCharsets.UTF_8.name()),
            new String(
                new TestResource(String.format("MultiRepodataUniqueTest/%s", res)).asBytes(),
                StandardCharsets.UTF_8
            ),
            true
        );
    }

    @Test
    void doesNothingIfOnePackageProvided() throws UnsupportedEncodingException, JSONException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        new MultiRepodata.Unique().merge(
            new ListOf<InputStream>(new TestResource("repodata.json").asInputStream()), out
        );
        JSONAssert.assertEquals(
            out.toString(StandardCharsets.UTF_8.name()),
            new String(new TestResource("repodata.json").asBytes(), StandardCharsets.UTF_8),
            true
        );
    }

    @Test
    void excludesDuplicates() throws UnsupportedEncodingException, JSONException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        new MultiRepodata.Unique().merge(
            new ListOf<InputStream>(
                this.resourceStream("exludesDupl_input1.json"),
                this.resourceStream("exludesDupl_input2.json"),
                this.resourceStream("exludesDupl_input3.json")
            ), out
        );
        JSONAssert.assertEquals(
            out.toString(StandardCharsets.UTF_8.name()),
            new String(
                new TestResource("MultiRepodataUniqueTest/exludesDupl_res.json").asBytes(),
                StandardCharsets.UTF_8
            ),
            true
        );
    }

    private InputStream resourceStream(final String name) {
        return new TestResource(String.format("MultiRepodataUniqueTest/%s", name))
            .asInputStream();
    }
}
