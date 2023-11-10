/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.npm;

import com.artipie.ArtipieException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.beans.HasPropertyWithValue;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Make sure the library is compatible with npm cli tools.
 *
 * @since 0.1
 */
final class RelativePathTest {

    /**
     * URL.
     */
    private static final String URL =
        "http://localhost:8080/artifactory/api/npm/npm-test-local-1/%s";

    @ParameterizedTest
    @ValueSource(strings = {
        "@scope/yuanye05/-/@scope/yuanye05-1.0.3.tgz",
        "@test/test.suffix/-/@test/test.suffix-5.5.3.tgz",
        "@my-org/test_suffix/-/@my-org/test_suffix-5.5.3.tgz",
        "@01.02_03/one_new.package/-/@01.02_03/one_new.package-9.0.3.tgz"
    })
    void npmClientWithScopeIdentifiedCorrectly(final String name) {
        MatcherAssert.assertThat(
            new TgzRelativePath(String.format(RelativePathTest.URL, name)).relative(),
            new IsEqual<>(name)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "yuanye05/-/yuanye05-1.0.3.tgz",
        "test.suffix/-/test.suffix-5.5.3.tgz",
        "test_suffix/-/test_suffix-5.5.3.tgz"
    })
    void npmClientWithoutScopeIdentifiedCorrectly(final String name) {
        MatcherAssert.assertThat(
            new TgzRelativePath(String.format(RelativePathTest.URL, name)).relative(),
            new IsEqual<>(name)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "@scope/yuanye05/yuanye05-1.0.3.tgz",
        "@my-org/test.suffix/test.suffix-5.5.3.tgz",
        "@test-org-test/test/-/test-1.0.0.tgz",
        "@a.b-c_01/test/-/test-0.1.0.tgz",
        "@thepeaklab/angelis/0.3.0/angelis-0.3.0.tgz",
        "@aa/bb/0.3.1/@aa/bb-0.3.1.tgz",
        "@a_a-b/bb/0.3.1/@a_a-b/bb-0.3.1.tgz",
        "@aa/bb/0.3.1-alpha/@aa/bb-0.3.1-alpha.tgz",
        "@aa/bb.js/0.3.1-alpha/@aa/bb.js-0.3.1-alpha.tgz"
    })
    void curlWithScopeIdentifiedCorrectly(final String name) {
        MatcherAssert.assertThat(
            new TgzRelativePath(String.format(RelativePathTest.URL, name)).relative(),
            new IsEqual<>(name)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "yuanye05/yuanye05-1.0.3.tgz",
        "yuanye05/1.0.3/yuanye05-1.0.3.tgz",
        "test.suffix/test.suffix-5.5.3.tgz",
        "test.suffix/5.5.3/test.suffix-5.5.3.tgz"
    })
    void curlWithoutScopeIdentifiedCorrectly(final String name) {
        MatcherAssert.assertThat(
            new TgzRelativePath(String.format(RelativePathTest.URL, name)).relative(),
            new IsEqual<>(name)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "foo\\bar-1.0.3.tgz",
        ""
    })
    void throwsForInvalidPaths(final String name) {
        final TgzRelativePath path = new TgzRelativePath(
            String.format(RelativePathTest.URL, name)
        );
        MatcherAssert.assertThat(
            Assertions.assertThrows(
                ArtipieException.class,
                path::relative
            ),
            new HasPropertyWithValue<>(
                "message",
                new StringContains(
                    "a relative path was not found"
                )
            )
        );
    }

    @ParameterizedTest
    @CsvSource({
        "yuanye05/-/yuanye05-1.0.3.tgz,yuanye05/1.0.3/yuanye05-1.0.3.tgz",
        "any.suf/-/any.suf-5.5.3-alpha.tgz,any.suf/5.5.3-alpha/any.suf-5.5.3-alpha.tgz",
        "test-some/-/test-some-5.5.3-rc1.tgz,test-some/5.5.3-rc1/test-some-5.5.3-rc1.tgz"
    })
    void replacesHyphenWithVersion(final String path, final String target) {
        MatcherAssert.assertThat(
            new TgzRelativePath(String.format(RelativePathTest.URL, path)).relative(true),
            new IsEqual<>(target)
        );
    }
}
