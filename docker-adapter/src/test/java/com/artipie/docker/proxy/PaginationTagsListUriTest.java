/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.proxy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for pagination tags list uri.
 */
class PaginationTagsListUriTest {

    @ParameterizedTest
    @CsvSource({
        "library/busybox,,0,/v2/library/busybox/tags/list",
        "dotnet/runtime,,10,/v2/dotnet/runtime/tags/list?n=10",
        "my-alpine,1.0,20,/v2/my-alpine/tags/list?n=20&last=1.0"
    })
    void shouldBuildPathString(String repo, String from, int limit, String uri) {
        Assertions.assertEquals(uri, ProxyManifests.uri(repo, limit, from));
    }
}
