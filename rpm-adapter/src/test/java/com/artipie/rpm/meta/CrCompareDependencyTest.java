/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm.meta;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CrCompareDependency}.
 *
 * @since 1.9.9
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class CrCompareDependencyTest {

    @Test
    void comparesSameDependencies() {
        MatcherAssert.assertThat(
            new CrCompareDependency().compare("libc.so.6", "libc.so.6"),
            Matchers.is(0)
        );
    }

    @Test
    void comparesDependencies() {
        MatcherAssert.assertThat(
            Arrays.asList(
                "libc.so.6(GLIBC_2.3.4)",
                "libc.so.6(GLIBC_2.4)",
                "libc.so.6(GLIBC_2.1.3)",
                "libc.so.6(GLIBC_2.3)",
                "libc.so.6(GLIBC_2.1.1)",
                "libc.so.6(GLIBC_2.2)",
                "libc.so.6",
                "libc.so.6(GLIBC_2.1)",
                "libc.so.6(GLIBC_2.0)"
            )
            .stream()
            .sorted(new CrCompareDependency())
            .collect(Collectors.joining(" < ")),
            Matchers.is(
                "libc.so.6 < libc.so.6(GLIBC_2.0) < libc.so.6(GLIBC_2.1) < libc.so.6(GLIBC_2.1.1) < libc.so.6(GLIBC_2.1.3) < libc.so.6(GLIBC_2.2) < libc.so.6(GLIBC_2.3) < libc.so.6(GLIBC_2.3.4) < libc.so.6(GLIBC_2.4)"
            )
        );
    }

    @Test
    void comparesDependenciesWithSameArchitecture() {
        MatcherAssert.assertThat(
            Arrays.asList(
                "libc.so.6(GLIBC_2.14)(64bit)",
                "libc.so.6(GLIBC_2.4)(64bit)",
                "libc.so.6(GLIBC_2.3)(64bit)",
                "libc.so.6(GLIBC_2.3.4)(64bit)",
                "libc.so.6()(64bit)",
                "libc.so.6(GLIBC_2.2.5)(64bit)"
            )
            .stream()
            .sorted(new CrCompareDependency())
            .collect(Collectors.joining(" < ")),
            Matchers.is(
                "libc.so.6()(64bit) < libc.so.6(GLIBC_2.2.5)(64bit) < libc.so.6(GLIBC_2.3)(64bit) < libc.so.6(GLIBC_2.3.4)(64bit) < libc.so.6(GLIBC_2.4)(64bit) < libc.so.6(GLIBC_2.14)(64bit)"
            )
        );
    }

    @Test
    void comparesDependenciesWithMixedArchitecture() {
        MatcherAssert.assertThat(
            Arrays.asList(
                "libc.so.6(GLIBC_2.3.4)(64 bit)",
                "libc.so.6(GLIBC_2.5)(32 bit)",
                "libc.so.6()",
                "libc.so.6(GLIBC_2.4)"
            )
            .stream()
            .sorted(new CrCompareDependency())
            .collect(Collectors.joining(" < ")),
            Matchers.is(
                "libc.so.6() < libc.so.6(GLIBC_2.3.4)(64 bit) < libc.so.6(GLIBC_2.4) < libc.so.6(GLIBC_2.5)(32 bit)"
            )
        );
    }

    @Test
    void comparesWithAtLeastTwoWrongDependencyNames() {
        final IllegalArgumentException thrown =
            Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> Arrays.asList(
                    "libc.so.6(",
                    "libc.so.6(GLIBC_2.5",
                    "libc.so.6(GLIBC_2.3.4)(64 bit)"
                )
                .stream()
                .sorted(new CrCompareDependency())
                .collect(Collectors.joining(" < "))
            );
        Assertions.assertEquals(
            "Wrong format for the names of dependencies !",
            thrown.getMessage()
        );
    }
}
