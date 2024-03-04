/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.pypi.http;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.client.HttpClientSettings;
import com.artipie.http.client.jetty.JettyClientSlices;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.pypi.PypiDeployment;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import org.apache.commons.io.IOUtils;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Test for {@link PyProxySlice}.
 * @since 0.7
 */
@DisabledOnOs(OS.WINDOWS)
final class PyProxySliceITCase {

    /**
     * Vertx instance.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Jetty client.
     */
    private final JettyClientSlices client = new JettyClientSlices(
        new HttpClientSettings().setFollowRedirects(true)
    );

    /**
     * Vertx slice server instance.
     */
    private VertxSliceServer server;

    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * Pypi container.
     */
    @RegisterExtension
    private final PypiDeployment container = new PypiDeployment();

    @BeforeEach
    void setUp() {
        this.client.start();
        this.storage = new InMemoryStorage();
        this.server = new VertxSliceServer(
            PyProxySliceITCase.VERTX,
            new LoggingSlice(
                new PyProxySlice(
                    this.client, URI.create("https://pypi.org/simple"), this.storage
                )
            ),
            this.container.port()
        );
        this.server.start();
    }

    @Test
    void installsFromProxy() throws IOException, InterruptedException {
        MatcherAssert.assertThat(
            this.container.bash(
                String.format(
                    "pip install --index-url %s --verbose --no-deps --trusted-host host.testcontainers.internal \"alarmtime\"",
                    this.container.localAddress()
                )
            ),
            Matchers.containsString("Successfully installed alarmtime-0.1.4")
        );
        MatcherAssert.assertThat(
            "Requested items cached",
            this.storage.list(new Key.From("alarmtime")).join().isEmpty(),
            new IsEqual<>(false)
        );
    }

    @Test
    void proxiesIndexRequest() throws Exception {
        final String key = "a2utils";
        final HttpURLConnection con = (HttpURLConnection) URI.create(
            String.format("http://localhost:%s/%s/", this.container.port(), key)
        ).toURL().openConnection();
        con.setRequestMethod(RqMethod.GET.value());
        MatcherAssert.assertThat(
            "Response status is 200",
            con.getResponseCode(),
            new IsEqual<>(Integer.parseInt(RsStatus.OK.code()))
        );
        final ListOf<String> expected = new ListOf<>(
            "<!DOCTYPE html>", "Links for a2utils",
            "a2utils-0.0.1-py3-none-any.whl", "a2utils-0.0.2-py3-none-any.whl"
        );
        MatcherAssert.assertThat(
            "Response body is html with packages list",
            IOUtils.toString(con.getInputStream(), StandardCharsets.UTF_8),
            new StringContainsInOrder(expected)
        );
        MatcherAssert.assertThat(
            "Index page was added to storage",
            this.storage.value(new Key.From(key)).join().asString(),
            new StringContainsInOrder(expected)
        );
        con.disconnect();
    }

    @Test
    void proxiesUnsuccessfulResponseStatus() throws Exception {
        final HttpURLConnection con = (HttpURLConnection) URI.create(
            String.format("http://localhost:%s/abc/123/", this.container.port())
        ).toURL().openConnection();
        con.setRequestMethod(RqMethod.GET.value());
        MatcherAssert.assertThat(
            "Response status is 404",
            con.getResponseCode(),
            new IsEqual<>(Integer.parseInt(RsStatus.NOT_FOUND.code()))
        );
        MatcherAssert.assertThat(
            "Nothing was added to storage",
            this.storage.list(Key.ROOT).join().isEmpty()
        );
        con.disconnect();
    }

    @Test
    void followsRedirects() throws Exception {
        final HttpURLConnection con = (HttpURLConnection) URI.create(
            String.format("http://localhost:%s/AlarmTime/", this.container.port())
        ).toURL().openConnection();
        con.setRequestMethod(RqMethod.GET.value());
        MatcherAssert.assertThat(
            "Response status is 200",
            con.getResponseCode(),
            new IsEqual<>(Integer.parseInt(RsStatus.OK.code()))
        );
        MatcherAssert.assertThat(
            "Alarm time index page was added to storage",
            this.storage.value(new Key.From("alarmtime")).join().asString(),
            new StringContainsInOrder(new ListOf<>("<!DOCTYPE html>", "Links for alarmtime"))
        );
        con.disconnect();
    }

    @AfterEach
    void tearDown() {
        this.client.stop();
        this.server.stop();
    }

}
