/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
 * @todo #429:30min Test `SystemSettings.trustAll` method.
 *  `SystemSettings.trustAll` reads data using static method `System.getenv`.
 *  To test this method in presence of this environment variable it is need to mock static method.
 *  This could be done with Powermock library, however it is tricky at the moment to use it with
 *  JUnit5 as it is not directly supported.
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
