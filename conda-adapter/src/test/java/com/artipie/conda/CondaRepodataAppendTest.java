/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.conda;

import com.artipie.asto.test.TestResource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.cactoos.list.ListOf;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * Test for {@link CondaRepodata.Remove}.
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class CondaRepodataAppendTest {

    @Test
    void addsPackagesToEmptyInput() throws UnsupportedEncodingException, JSONException {
        final ByteArrayOutputStream res = new ByteArrayOutputStream();
        new CondaRepodata.Append(res).perform(
            new ListOf<CondaRepodata.PackageItem>(
                new CondaRepodata.PackageItem(
                    new TestResource("7zip-19.00-h59b6b97_2.conda").asInputStream(),
                    "7zip-19.00-h59b6b97_2.conda", "7zip-sha256", "7zip-md5", 123L
                ),
                new CondaRepodata.PackageItem(
                    new TestResource("anaconda-navigator-1.8.4-py35_0.tar.bz2").asInputStream(),
                    "anaconda-navigator-1.8.4-py35_0.tar.bz2", "conda-navi-sha256",
                    "conda-navi-md5", 876L
                )
            )
        );
        JSONAssert.assertEquals(
            new String(
                new TestResource("CondaRepodataAppendTest/addsPackagesToEmptyInput.json").asBytes(),
                StandardCharsets.UTF_8
            ),
            res.toString(StandardCharsets.UTF_8.name()),
            true
        );
    }

    @Test
    void appendsPackages() throws IOException, JSONException {
        final ByteArrayOutputStream res = new ByteArrayOutputStream();
        try (InputStream input = new TestResource("repodata.json").asInputStream()) {
            new CondaRepodata.Append(input, res).perform(
                new ListOf<CondaRepodata.PackageItem>(
                    new CondaRepodata.PackageItem(
                        new TestResource("7zip-19.00-h59b6b97_2.conda").asInputStream(),
                        "7zip-19.00-h59b6b97_2.conda", "7zip-sha256", "7zip-md5", 123L
                    ),
                    new CondaRepodata.PackageItem(
                        new TestResource("anaconda-navigator-1.8.4-py35_0.tar.bz2").asInputStream(),
                        "anaconda-navigator-1.8.4-py35_0.tar.bz2",
                        "conda-navi-sha256", "conda-navi-md5", 876L
                    )
                )
            );
        }
        JSONAssert.assertEquals(
            new String(
                new TestResource("CondaRepodataAppendTest/appendsPackages.json").asBytes(),
                StandardCharsets.UTF_8
            ),
            res.toString(StandardCharsets.UTF_8.name()),
            true
        );
    }

    @Test
    void doesNothingIfItemsAreEmpty() throws IOException, JSONException {
        final ByteArrayOutputStream res = new ByteArrayOutputStream();
        try (InputStream input = new TestResource("repodata.json").asInputStream()) {
            new CondaRepodata.Append(input, res).perform(Collections.emptyList());
        }
        JSONAssert.assertEquals(
            new String(new TestResource("repodata.json").asBytes(), StandardCharsets.UTF_8),
            res.toString(StandardCharsets.UTF_8.name()),
            true
        );
    }
}
