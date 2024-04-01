/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http;

import com.artipie.files.FileProxySlice;
import com.artipie.http.client.jetty.JettyClientSlices;
import com.artipie.http.group.GroupSlice;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.http.client.utils.URIBuilder;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Integration tests for grouped repositories.
 * @since 0.10
 * @todo #370:30min Enable `test.networkEnabled` property for some CI builds.
 *  Make sure these tests are not failing due to network issues, maybe we should retry
 *  it to avoid false failures.
 */
@EnabledIfSystemProperty(named = "test.networkEnabled", matches = "true|yes|on|1")
final class GroupRepositoryITCase {

    /**
     * Http clients for proxy slice.
     */
    private final JettyClientSlices clients = new JettyClientSlices();

    @BeforeEach
    void setUp() throws Exception {
        this.clients.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        this.clients.stop();
    }

    @Test
    void fetchesCorrectContentFromGroupedFilesProxy() throws Exception {
        MatcherAssert.assertThat(
            new GroupSlice(
                this.proxy("/artipie/none-2/"),
                this.proxy("/artipie/tests/"),
                this.proxy("/artipie/none-1/")
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.OK),
                new RequestLine(
                    RqMethod.GET, URI.create("/GroupRepositoryITCase-one.txt").toString()
                )
            )
        );
    }

    private Slice proxy(final String path) throws URISyntaxException {
        return new FileProxySlice(
            this.clients,
            new URIBuilder(URI.create("https://central.artipie.com"))
                .setPath(path)
                .build()
        );
    }
}
