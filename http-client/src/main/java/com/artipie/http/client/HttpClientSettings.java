/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.client;

import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlSequence;
import com.google.common.base.Strings;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

/**
 * Http client settings.
 * @since 0.2
 */
public class HttpClientSettings {

    public static HttpClientSettings from(YamlMapping mapping) {
        final HttpClientSettings res = new HttpClientSettings();
        if (mapping != null) {
            final String conTimeout = mapping.string("connection_timeout");
            if (!Strings.isNullOrEmpty(conTimeout)) {
                res.setConnectTimeout(Long.parseLong(conTimeout));
            }
            final String idleTimeout = mapping.string("idle_timeout");
            if (!Strings.isNullOrEmpty(idleTimeout)) {
                res.setIdleTimeout(Long.parseLong(idleTimeout));
            }
            final String trustAll = mapping.string("trust_all");
            if (!Strings.isNullOrEmpty(trustAll)) {
                res.setTrustAll(Boolean.parseBoolean(trustAll));
            }
            final String http3 = mapping.string("http3");
            if (!Strings.isNullOrEmpty(http3)) {
                res.setHttp3(Boolean.parseBoolean(http3));
            }
            final String followRedirects = mapping.string("follow_redirects");
            if (!Strings.isNullOrEmpty(followRedirects)) {
                res.setFollowRedirects(Boolean.parseBoolean(followRedirects));
            }
            final YamlMapping jks = mapping.yamlMapping("jks");
            if (jks != null) {
                res.setJksPath(
                    Objects.requireNonNull(jks.string("path"),
                        "'path' element is not in mapping `jks` settings")
                    )
                    .setJksPwd(jks.string("password"));
            }
            final YamlSequence proxies = mapping.yamlSequence("proxies");
            if (proxies != null) {
                StreamSupport.stream(proxies.spliterator(), false)
                    .forEach(proxy -> {
                        if (proxy instanceof YamlMapping yml) {
                            res.addProxy(ProxySettings.from(yml));
                        } else {
                            throw new IllegalStateException(
                                "`proxies` element is not mapping in meta config"
                            );
                        }
                    });
            }
        }
        return res;
    }

    private static Optional<ProxySettings> proxySettingsFromSystem(String scheme) {
        final String host = System.getProperty(scheme + ".proxyHost");
        if (!Strings.isNullOrEmpty(host)) {
            int port = -1;
            final String httpPort = System.getProperty(scheme + ".proxyPort");
            if (!Strings.isNullOrEmpty(httpPort)) {
                port = Integer.parseInt(httpPort);
            }
            try {
                return Optional.of(new ProxySettings(scheme, host, port));
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return Optional.empty();
    }

    /**
     * Read HTTP proxy settings if enabled.
     */
    private final List<ProxySettings> proxies;

    /**
     * Determine if it is required to trust all SSL certificates.
     */
    private boolean trustAll;

    /**
     * Java key store path.
     */
    private String jksPath;

    /**
     * Java key store pwd.
     */
    private String jksPwd;

    /**
     * Determine if redirects should be followed.
     */
    private boolean followRedirects;

    /**
     * Use http3 transport.
     */
    private boolean http3;

    /**
     * Max time, in milliseconds, a connection can take to connect to destination.
     * Zero means infinite wait time.
     */
    private long connectTimeout;

    /**
     * The max time, in milliseconds, a connection can be idle (no incoming or outgoing traffic).
     * Zero means infinite wait time.
     */
    private long idleTimeout;

    public HttpClientSettings() {
        this.trustAll = false;
        this.followRedirects = false;
        this.connectTimeout = 15_000L;
        this.idleTimeout = 0L;
        this.http3 = false;
        this.proxies = new ArrayList<>();
        proxySettingsFromSystem("http")
            .ifPresent(this::addProxy);
        proxySettingsFromSystem("https")
            .ifPresent(this::addProxy);
    }

    public HttpClientSettings addProxy(ProxySettings ps) {
        proxies.add(ps);
        return this;
    }

    public List<ProxySettings> proxies() {
        return Collections.unmodifiableList(this.proxies);
    }

    public boolean trustAll() {
        return trustAll;
    }

    public HttpClientSettings setTrustAll(final boolean trustAll) {
        this.trustAll = trustAll;
        return this;
    }

    public String jksPath() {
        return jksPath;
    }

    public HttpClientSettings setJksPath(final String jksPath) {
        this.jksPath = jksPath;
        return this;
    }

    public String jksPwd() {
        return jksPwd;
    }

    public HttpClientSettings setJksPwd(final String jksPwd) {
        this.jksPwd = jksPwd;
        return this;
    }

    public boolean followRedirects() {
        return followRedirects;
    }

    public HttpClientSettings setFollowRedirects(final boolean followRedirects) {
        this.followRedirects = followRedirects;
        return this;
    }

    public boolean http3() {
        return http3;
    }

    public HttpClientSettings setHttp3(final boolean http3) {
        this.http3 = http3;
        return this;
    }

    public long connectTimeout() {
        return connectTimeout;
    }

    public HttpClientSettings setConnectTimeout(final long connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public long idleTimeout() {
        return idleTimeout;
    }

    public HttpClientSettings setIdleTimeout(final long idleTimeout) {
        this.idleTimeout = idleTimeout;
        return this;
    }
}
