/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm.pkg;

import com.artipie.ArtipieException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link HeaderTags.Version}.
 * @since 1.9
 */
class HeaderTagsVersionTest {

    @ParameterizedTest
    @CsvSource({
        "'',''",
        "5,5",
        "1.0,1.0",
        "1.0.1-26.git20200127.fc32,1.0.1",
        "2.0_1-2.jfh.sdd,2.0_1",
        "2:9.0.2,9.0.2",
        "1:1-9.878,1",
        "2.9+9(7),2.9+9(7)",
        "20120211-x86-64,20120211"
    })
    void readsVersion(final String val, final String res) {
        MatcherAssert.assertThat(
            new HeaderTags.Version(val).ver(),
            new IsEqual<>(res)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "1.0,0",
        "1.0.1-26.git20200127.fc32,0",
        "2:9.0.2,2",
        "1:1-9.878,1"
    })
    void readsEpoch(final String val, final String res) {
        MatcherAssert.assertThat(
            new HeaderTags.Version(val).epoch(),
            new IsEqual<>(res)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "1.0.1-26.git20200127.fc32,26.git20200127.fc32",
        "1:1-9.878,9.878",
        "20120211-x86-64,x86-64"
    })
    void readsRel(final String val, final String res) {
        MatcherAssert.assertThat(
            new HeaderTags.Version(val).rel().get(),
            new IsEqual<>(res)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "1.0.1_26.git20200127.fc32",
        "3:1.2.3pc",
        ""
    })
    void returnsEmptyWhenRelIsNotPresent(final String val) {
        MatcherAssert.assertThat(
            new HeaderTags.Version(val).rel().isPresent(),
            new IsEqual<>(false)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "1:1-test,0:1-test,1",
        "1:1-test,0:2-test,1",
        "0:1-test,1:1-test,-1",
        "1.0.1-26.git20200127.fc32,1.0.2-26.git20200127.fc32,-1",
        "3.0.1-test1,1.0.2-test3,1",
        "1.1-test1,1.1-test3,-1"
    })
    void comparesVersions(final String first, final String second, final int res) {
        MatcherAssert.assertThat(
            new HeaderTags.Version(first).compareTo(new HeaderTags.Version(second)),
            new IsEqual<>(res)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "-",
        "3:sd/sd-2.3alpha",
        "1/5",
        "3:1-1/1"
    })
    void throwsExceptionWhenVersionNotValid(final String param) {
        Assertions.assertThrows(
            ArtipieException.class,
            () -> new HeaderTags.Version(param).ver()
        );
    }

}
