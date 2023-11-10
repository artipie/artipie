/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.http.client;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link SettingsTest}.
 *
 * @since 0.1
 * @checkstyle MagicNumberCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.TooManyMethods")
final class SettingsTest {

    @Test
    void defaultProxy() {
        MatcherAssert.assertThat(
            new Settings.Default().proxy().isPresent(),
            new IsEqual<>(false)
        );
    }

    @Test
    void defaultTrustAll() {
        MatcherAssert.assertThat(
            new Settings.Default().trustAll(),
            new IsEqual<>(false)
        );
    }

    @Test
    void defaultFollowRedirects() {
        MatcherAssert.assertThat(
            new Settings.Default().followRedirects(),
            new IsEqual<>(false)
        );
    }

    @Test
    void defaultConnectTimeout() {
        final long millis = 15_000L;
        MatcherAssert.assertThat(
            new Settings.Default().connectTimeout(),
            new IsEqual<>(millis)
        );
    }

    @Test
    void defaultIdleTimeout() {
        MatcherAssert.assertThat(
            new Settings.Default().idleTimeout(),
            new IsEqual<>(0L)
        );
    }

    @Test
    void proxyFrom() {
        final boolean secure = true;
        final String host = "proxy.com";
        final int port = 8080;
        final Settings.Proxy.Simple proxy = new Settings.Proxy.Simple(secure, host, port);
        MatcherAssert.assertThat(
            "Wrong secure flag",
            proxy.secure(),
            new IsEqual<>(secure)
        );
        MatcherAssert.assertThat(
            "Wrong host",
            proxy.host(),
            new IsEqual<>(host)
        );
        MatcherAssert.assertThat(
            "Wrong port",
            proxy.port(),
            new IsEqual<>(port)
        );
    }

    @Test
    void withProxy() {
        final Settings.Proxy proxy = new Settings.Proxy.Simple(false, "example.com", 80);
        MatcherAssert.assertThat(
            new Settings.WithProxy(new Settings.Default(), proxy).proxy(),
            new IsEqual<>(Optional.of(proxy))
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void withTrustAll(final boolean value) {
        MatcherAssert.assertThat(
            new Settings.WithTrustAll(value).trustAll(),
            new IsEqual<>(value)
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void withFollowRedirects(final boolean value) {
        MatcherAssert.assertThat(
            new Settings.WithFollowRedirects(value).followRedirects(),
            new IsEqual<>(value)
        );
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 10, 20_000})
    void withConnectTimeout(final long value) {
        MatcherAssert.assertThat(
            new Settings.WithConnectTimeout(value).connectTimeout(),
            new IsEqual<>(value)
        );
    }

    @Test
    void withConnectTimeoutInSeconds() {
        MatcherAssert.assertThat(
            new Settings.WithConnectTimeout(5, TimeUnit.SECONDS).connectTimeout(),
            new IsEqual<>(5_000L)
        );
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 10, 20_000})
    void withIdleTimeout(final long value) {
        MatcherAssert.assertThat(
            new Settings.WithIdleTimeout(value).idleTimeout(),
            new IsEqual<>(value)
        );
    }

    @Test
    void withIdleTimeoutInSeconds() {
        MatcherAssert.assertThat(
            new Settings.WithIdleTimeout(5, TimeUnit.SECONDS).idleTimeout(),
            new IsEqual<>(5_000L)
        );
    }
}
