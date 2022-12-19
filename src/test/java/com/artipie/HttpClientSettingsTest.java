/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.artipie.http.client.Settings;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link HttpClientSettings}.
 *
 * @since 0.9
 */
class HttpClientSettingsTest {

    @Test
    public void shouldNotHaveProxy() {
        System.getProperties().remove(HttpClientSettings.PROXY_HOST);
        System.getProperties().remove(HttpClientSettings.PROXY_PORT);
        MatcherAssert.assertThat(
            new HttpClientSettings().proxy().isPresent(),
            new IsEqual<>(false)
        );
    }

    @Test
    public void shouldHaveProxyWhenSpecified() {
        final String host = "artipie.com";
        final int port = 1234;
        System.setProperty(HttpClientSettings.PROXY_HOST, host);
        System.setProperty(HttpClientSettings.PROXY_PORT, String.valueOf(port));
        final Optional<Settings.Proxy> proxy = new HttpClientSettings().proxy();
        MatcherAssert.assertThat(
            "Proxy enabled",
            proxy.isPresent(),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Proxy is not secure",
            proxy.get().secure(),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat(
            "Proxy has expected host",
            proxy.get().host(),
            new IsEqual<>(host)
        );
        MatcherAssert.assertThat(
            "Proxy has expected port",
            proxy.get().port(),
            new IsEqual<>(port)
        );
    }

    @Test
    public void shouldNotTrustAll() {
        MatcherAssert.assertThat(
            new HttpClientSettings().trustAll(),
            new IsEqual<>(false)
        );
    }

    @Test
    public void shouldFollowRedirects() {
        MatcherAssert.assertThat(
            new HttpClientSettings().followRedirects(),
            new IsEqual<>(true)
        );
    }
}
