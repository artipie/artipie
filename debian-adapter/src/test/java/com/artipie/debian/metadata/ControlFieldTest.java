/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.debian.metadata;

import java.util.NoSuchElementException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link ControlField}.
 * @since 0.1
 */
class ControlFieldTest {

    @Test
    void extractsArchitectureField() {
        MatcherAssert.assertThat(
            new ControlField.Architecture().value(
                String.join(
                    "\n",
                    "Package: aglfn",
                    "Version: 1.7-3",
                    "Architecture: all",
                    "Maintainer: Debian Fonts Task Force <pkg-fonts-devel@lists.alioth.debian.org>",
                    "Installed-Size: 138",
                    "Section: fonts"
                )
            ),
            Matchers.contains("all")
        );
    }

    @Test
    void extractsArchitecturesField() {
        MatcherAssert.assertThat(
            new ControlField.Architecture().value(
                String.join(
                    "\n",
                    "Package: abc",
                    "Version: 0.1",
                    "Architecture: amd64 amd32"
                )
            ),
            Matchers.contains("amd64", "amd32")
        );
    }

    @Test
    void extractsPackageField() {
        MatcherAssert.assertThat(
            new ControlField.Package().value(
                String.join(
                    "\n",
                    "Package: xyz",
                    "Version: 0.3",
                    "Architecture: amd64 intell"
                )
            ),
            Matchers.contains("xyz")
        );
    }

    @Test
    void extractsVersionField() {
        MatcherAssert.assertThat(
            new ControlField.Version().value(
                String.join(
                    "\n",
                    "Package: 123",
                    "Version: 0.987",
                    "Architecture: amd32"
                )
            ),
            Matchers.contains("0.987")
        );
    }

    @Test
    void throwsExceptionWhenElementNotFound() {
        Assertions.assertThrows(
            NoSuchElementException.class,
            () -> new ControlField.Architecture().value("invalid control")
        );
    }

}
