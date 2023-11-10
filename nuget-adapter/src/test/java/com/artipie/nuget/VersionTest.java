/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.nuget;

import com.artipie.nuget.metadata.Version;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link Version}.
 *
 * @since 0.1
 */
class VersionTest {

    @ParameterizedTest
    @CsvSource({
        "1.00,1.0",
        "1.01.1,1.1.1",
        "1.00.0.1,1.0.0.1",
        "1.0.0.0,1.0.0",
        "1.0.01.0,1.0.1",
        "0.0.4,0.0.4",
        "1.2.3,1.2.3",
        "10.20.30,10.20.30",
        "1.1.2-prerelease+meta,1.1.2-prerelease",
        "1.1.2+meta,1.1.2",
        "1.1.2+meta-valid,1.1.2",
        "1.0.0-alpha,1.0.0-alpha",
        "1.0.0-beta,1.0.0-beta",
        "1.0.0-alpha.beta,1.0.0-alpha.beta",
        "1.0.0-alpha.beta.1,1.0.0-alpha.beta.1",
        "1.0.0-alpha.1,1.0.0-alpha.1",
        "1.0.0-alpha0.valid,1.0.0-alpha0.valid",
        "1.0.0-alpha.0valid,1.0.0-alpha.0valid",
        "1.0.0-alpha-a.b-c-somethinglong+build.1-aef.1-its-okay,1.0.0-alpha-a.b-c-somethinglong",
        "1.0.0-rc.1+build.1,1.0.0-rc.1",
        "2.0.0-rc.1+build.123,2.0.0-rc.1",
        "1.2.3-beta,1.2.3-beta",
        "10.2.3-DEV-SNAPSHOT,10.2.3-DEV-SNAPSHOT",
        "1.2.3-SNAPSHOT-123,1.2.3-SNAPSHOT-123",
        "1.0.0,1.0.0",
        "2.0.0,2.0.0",
        "1.1.7,1.1.7",
        "2.0.0+build.1848,2.0.0",
        "2.0.1-alpha.1227,2.0.1-alpha.1227",
        "1.0.0-alpha+beta,1.0.0-alpha",
        "1.2.3----RC-SNAPSHOT.12.9.1--.12+788,1.2.3----RC-SNAPSHOT.12.9.1--.12",
        "1.2.3----R-S.12.9.1--.12+meta,1.2.3----R-S.12.9.1--.12",
        "1.2.3----RC-SNAPSHOT.12.9.1--.12,1.2.3----RC-SNAPSHOT.12.9.1--.12",
        "1.0.0+0.build.1-rc.10000aaa-kk-0.1,1.0.0",
        //@checkstyle LineLengthCheck (1 line)
        "99999999999999999999999.999999999999999999.99999999999999999,99999999999999999999999.999999999999999999.99999999999999999",
        "1.0.0-0A.is.legal,1.0.0-0A.is.legal"
    })
    void shouldNormalize(final String original, final String expected) {
        final Version version = new Version(original);
        MatcherAssert.assertThat(
            version.normalized(),
            Matchers.equalTo(expected)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "1",
        "1.1.2+.123",
        "+invalid",
        "-invalid",
        "-invalid+invalid",
        "-invalid.01",
        "alpha",
        "alpha.beta",
        "alpha.beta.1",
        "alpha.1",
        "alpha+beta",
        "alpha_beta",
        "alpha.",
        "alpha..",
        "beta",
        "1.0.0-alpha_beta",
        "-alpha.",
        "1.0.0-alpha..",
        "1.0.0-alpha..1",
        "1.0.0-alpha...1",
        "1.0.0-alpha....1",
        "1.0.0-alpha.....1",
        "1.0.0-alpha......1",
        "1.0.0-alpha.......1",
        "1.2.3.DEV",
        "1.2.31.2.3----RC-SNAPSHOT.12.09.1--..12+788",
        "+justmeta",
        "9.8.7+meta+meta",
        "9.8.7-whatever+meta+meta",
        //@checkstyle LineLengthCheck (1 line)
        "99999999999999999999999.999999999999999999.99999999999999999----RC-SNAPSHOT.12.09.1--------------------------------..12"
    })
    void shouldNotNormalize(final String original) {
        final Version version = new Version(original);
        Assertions.assertThrows(RuntimeException.class, version::normalized);
    }

    @ParameterizedTest
    @CsvSource({
        "1.0,false",
        "1.2.3-beta,false",
        "1.2.3-alfa.1,true",
        "1.1.2-prerelease+meta,true",
        "1.1.2-prerelease.2+meta,true",
        "1.1.2+meta,true",
        "1.2.3-SNAPSHOT-123,false"
    })
    void definesSemVerTwoVersions(final String ver, final boolean res) {
        MatcherAssert.assertThat(
            new Version(ver).isSemVerTwo(),
            new IsEqual<>(res)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "1.0,false",
        "1.2.3-beta,true",
        "1.2.3-alpha.1,true",
        "1.1.2-alpha+meta,true",
        "1.1.2+meta,false",
        "1.2.3-SNAPSHOT-123,true"
    })
    void definesPreReleaseVersions(final String ver, final boolean res) {
        MatcherAssert.assertThat(
            new Version(ver).isPrerelease(),
            new IsEqual<>(res)
        );
    }

    @ParameterizedTest
    @MethodSource("pairs")
    void shouldBeLessThenGreater(final String lesser, final String greater) {
        MatcherAssert.assertThat(new Version(lesser), Matchers.lessThan(new Version(greater)));
    }

    @ParameterizedTest
    @MethodSource("pairs")
    void shouldBeGreaterThenLesser(final String lesser, final String greater) {
        MatcherAssert.assertThat(new Version(greater), Matchers.greaterThan(new Version(lesser)));
    }

    @ParameterizedTest
    @MethodSource("versions")
    void shouldBeCompareEqualToSelf(final String version) {
        MatcherAssert.assertThat(
            new Version(version),
            Matchers.comparesEqualTo(new Version(version))
        );
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private static Stream<Object[]> pairs() {
        return orderedSequences().flatMap(
            ordered -> IntStream.range(0, ordered.length).mapToObj(
                lesser -> IntStream.range(lesser + 1, ordered.length).mapToObj(
                    greater -> new Object[] {ordered[lesser], ordered[greater]}
                )
            )
        ).flatMap(Function.identity());
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private static Stream<String> versions() {
        return orderedSequences().map(Stream::of).flatMap(pairs -> pairs);
    }

    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    private static Stream<String[]> orderedSequences() {
        return Stream.of(
            new String[] {"0.1", "0.2", "0.11", "1.0", "2.0", "2.1", "18.0"},
            new String[] {"3.0", "3.0.1", "3.0.2", "3.0.10", "3.1"},
            new String[] {"4.0.1", "4.0.1.1", "4.0.1.2", "4.0.1.17", "4.0.2"},
            new String[] {
                "1.0.0-alpha",
                "1.0.0-alpha.1",
                "1.0.0-alpha.beta",
                "1.0.0-beta",
                "1.0.0-beta.2",
                "1.0.0-beta.11",
                "1.0.0-rc.1",
                "1.0.0",
            }
        );
    }
}
