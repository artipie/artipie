/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.pypi.meta;

import com.artipie.asto.test.TestResource;
import java.io.ByteArrayInputStream;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test for {@link Metadata.FromArchive}.
 * @since 0.6
 */
class MetadataFromArchiveTest {

    @ParameterizedTest
    @CsvSource({
        "pypi_repo/artipie-sample-0.2.zip",
        "pypi_repo/artipie-sample-0.2.tar",
        "pypi_repo/artipie-sample-0.2.tar.gz",
        "pypi_repo/artipie-sample-2.1.tar.Z",
        "pypi_repo/artipie-sample-2.1.tar.bz2",
        "pypi_repo/artipie_sample-2.1-py3.7.egg",
        "pypi_repo/artipie_sample-0.2-py3-none-any.whl"
    })
    void readsFromTarGz(final String filename) {
        MatcherAssert.assertThat(
            new Metadata.FromArchive(
                new TestResource(filename).asInputStream(), filename
            ).read().name(),
            new IsEqual<>("artipie-sample")
        );
    }

    @Test
    void throwsExceptionIfArchiveIsUnsupported() {
        Assertions.assertThrows(
            UnsupportedOperationException.class,
            () -> new Metadata.FromArchive(
                new ByteArrayInputStream("any".getBytes()), "some/archive.tar.br"
            ).read()
        );
    }

}
