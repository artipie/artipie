/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.http.client;

import com.amihaiemil.eoyaml.YamlMapping;
import com.google.common.base.Strings;
import org.apache.hc.core5.net.URIBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * Proxy settings.
 *
 * @since 0.2
 */
public class ProxySettings {

    public static ProxySettings from(final YamlMapping yaml) {
        final URI uri = URI.create(
            Objects.requireNonNull(
                yaml.string("url"),
                "`url` is not specified for proxy remote"
            )
        );
        final ProxySettings res = new ProxySettings(uri);
        final String realm = yaml.string("realm");
        if (!Strings.isNullOrEmpty(realm)) {
            res.setBasicRealm(realm);
            res.setBasicUser(
                Objects.requireNonNull(
                    yaml.string("username"),
                    "`username` is not specified for \"Basic\" authentication"
                )
            );
            res.setBasicPwd(yaml.string("password"));
        }
        return res;
    }

    private final URI uri;

    private String basicRealm;

    private String basicUser;

    private String basicPwd;

    /**
     * Ctor.
     *
     * @param uri Proxy url.
     */
    public ProxySettings(final URI uri) {
        this.uri = uri;
    }

    public ProxySettings(
        final String scheme,
        final String host,
        final int port
    ) throws URISyntaxException {
        this(
            new URIBuilder()
                .setScheme(scheme)
                .setHost(host)
                .setPort(port)
                .build()
        );
    }

    /**
     * Proxy URI.
     *
     * @return URI.
     */
    public URI uri() {
        return this.uri;
    }

    /**
     * Proxy host.
     *
     * @return Host.
     */
    public String host() {
        return this.uri.getHost();
    }

    /**
     * Proxy port.
     *
     * @return Port.
     */
    public int port() {
        return this.uri.getPort();
    }

    /**
     * Read if proxy is secure.
     *
     * @return If proxy should be accessed via HTTPS protocol <code>true</code> is returned,
     *  <code>false</code> - for unsecure HTTP proxies.
     */
    public boolean secure() {
        return Objects.equals(this.uri.getScheme(), "https");
    }

    /**
     * The realm to match for the authentication.
     *
     * @return Realm.
     */
    public String basicRealm() {
        return basicRealm;
    }

    public void setBasicRealm(final String basicRealm) {
        this.basicRealm = basicRealm;
    }

    /**
     * The user that wants to authenticate.
     *
     * @return Username.
     */
    public String basicUser() {
        return basicUser;
    }

    public void setBasicUser(final String basicUser) {
        this.basicUser = basicUser;
    }

    /**
     * The password of the user.
     *
     * @return Password.
     */
    public String basicPwd() {
        return basicPwd;
    }

    public void setBasicPwd(final String basicPwd) {
        this.basicPwd = basicPwd;
    }
}
