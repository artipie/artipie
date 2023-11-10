/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.jetty.http3;

import com.amihaiemil.eoyaml.YamlMapping;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * Ssl for http3 from yaml configuration file.
 * Yaml format:
 * <pre>{@code
 * http3_ssl:
 *   jks:
 *     path: keystore.jks
 *     password: secret
 * }</pre>
 * @since 0.31
 */
public final class SslFactoryFromYaml {

    /**
     * Jks keystore type.
     */
    private static final String JKS = "jks";

    /**
     * Yaml mapping.
     */
    private final YamlMapping yaml;

    /**
     * Ctor.
     * @param yaml Yaml mapping
     */
    public SslFactoryFromYaml(final YamlMapping yaml) {
        this.yaml = yaml;
    }

    /**
     * Builds ssl context factory. If jks section or some fields are absent in yaml
     * configuration, then empty {@link SslContextFactory.Server} object is returned. Jetty ssl
     * can also be configured via system variables, check {@link SslContextFactory}
     * for more information.
     * @return Instance of {@link SslContextFactory.Server}.
     */
    public SslContextFactory.Server build() {
        final SslContextFactory.Server res = new SslContextFactory.Server();
        final YamlMapping sectionssl = this.yaml.yamlMapping("http3_ssl");
        if (sectionssl != null && sectionssl.yamlMapping(SslFactoryFromYaml.JKS) != null) {
            final YamlMapping jks = sectionssl.yamlMapping(SslFactoryFromYaml.JKS);
            res.setKeyStoreType(SslFactoryFromYaml.JKS);
            res.setKeyStorePath(jks.string("path"));
            res.setKeyStorePassword(jks.string("password"));
        }
        return res;
    }
}
