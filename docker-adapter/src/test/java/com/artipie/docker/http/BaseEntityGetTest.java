/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.docker.http;

import com.artipie.asto.Content;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.docker.asto.AstoDocker;
import com.artipie.http.Headers;
import com.artipie.http.ResponseImpl;
import com.artipie.http.headers.Header;
import com.artipie.http.hm.ResponseAssert;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DockerSlice}.
 * Base GET endpoint.
 */
class BaseEntityGetTest {

    private DockerSlice slice;

    @BeforeEach
    void setUp() {
        this.slice = new DockerSlice(new AstoDocker(new InMemoryStorage()));
    }

    @Test
    void shouldRespondOkToVersionCheck() {
        final ResponseImpl response = this.slice
            .response(new RequestLine(RqMethod.GET, "/v2/"), Headers.EMPTY, Content.EMPTY)
            .join();
        ResponseAssert.check(response, RsStatus.OK,
            new Header("Docker-Distribution-API-Version", "registry/2.0"));
    }
}
