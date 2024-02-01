/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.Key;
import com.artipie.asto.test.TestResource;
import com.artipie.http.client.HttpClientSettings;
import com.artipie.http.client.ProxySettings;
import com.artipie.scheduling.QuartzService;
import com.artipie.settings.StorageByAlias;
import com.artipie.settings.YamlSettings;
import com.artipie.settings.repo.RepoConfig;
import com.artipie.test.TestStoragesCache;
import java.net.URI;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link HttpClientSettings}.
 *
 * @since 0.9
 */
class HttpClientSettingsTest {

    private static void removeProxyProperties(){
        System.getProperties().remove("http.proxyHost");
        System.getProperties().remove("http.proxyPort");
        System.getProperties().remove("https.proxyHost");
        System.getProperties().remove("https.proxyPort");
    }

    @AfterEach
    void tearDown() {
        removeProxyProperties();
    }

    @Test
    void shouldNotHaveProxyByDefault() {
        removeProxyProperties();
        Assertions.assertTrue(new HttpClientSettings().proxies().isEmpty());
    }

    @Test
    void shouldHaveProxyFromSystemWhenSpecified() {
        System.setProperty("http.proxyHost", "notsecure.com");
        System.setProperty("http.proxyPort", "1234");
        System.setProperty("https.proxyHost", "secure.com");
        System.setProperty("https.proxyPort", "6778");
        final HttpClientSettings settings = new HttpClientSettings();
        Assertions.assertEquals(2, settings.proxies().size());
        for (ProxySettings proxy : settings.proxies()) {
            switch (proxy.host()) {
                case "notsecure.com": {
                    Assertions.assertFalse(proxy.secure());
                    Assertions.assertEquals(1234, proxy.port());
                    break;
                }
                case "secure.com": {
                    Assertions.assertTrue(proxy.secure());
                    Assertions.assertEquals(6778, proxy.port());
                    break;
                }
                default:
                    Assertions.fail("Unexpected host name: " + proxy.host());
            }
        }
    }

    @Test
    void shouldInitFromMetaYaml() throws Exception {
        final Path path = new TestResource("artipie_http_client.yaml").asPath();
        final HttpClientSettings stn = new YamlSettings(
            Yaml.createYamlInput(path.toFile()).readYamlMapping(),
            path.getParent(), new QuartzService()
        ).httpClientSettings();
        Assertions.assertEquals(20_000, stn.connectTimeout());
        Assertions.assertEquals(25, stn.idleTimeout());
        Assertions.assertTrue(stn.trustAll());
        Assertions.assertTrue(stn.followRedirects());
        Assertions.assertTrue(stn.http3());
        Assertions.assertEquals("/var/artipie/keystore.jks", stn.jksPath());
        Assertions.assertEquals("secret", stn.jksPwd());
        Assertions.assertEquals(stn.proxies().size(), 2);
        final ProxySettings proxy = stn.proxies().get(0);
        Assertions.assertEquals(URI.create("https://proxy1.com"), proxy.uri());
        Assertions.assertEquals("user_realm", proxy.basicRealm());
        Assertions.assertEquals("user_name", proxy.basicUser());
        Assertions.assertEquals("user_password", proxy.basicPwd());
    }

    @Test
    void shouldInitRepoConfigFromFile() throws Exception {
        final RepoConfig cfg = new RepoConfig(
            new StorageByAlias(Yaml.createYamlMappingBuilder().build()),
            new Key.From("aaa"),
            Yaml.createYamlInput(
                new TestResource("docker/docker-proxy-http-client.yml").asInputStream()
            ).readYamlMapping(),
            new TestStoragesCache(), false
        );
        final HttpClientSettings stn = cfg.httpClientSettings()
            .orElseGet(() -> Assertions.fail("Should return HttpClientSettings"));
        Assertions.assertEquals(25000, stn.connectTimeout());
        Assertions.assertEquals(500, stn.idleTimeout());
        Assertions.assertTrue(stn.trustAll());
        Assertions.assertTrue(stn.followRedirects());
        Assertions.assertTrue(stn.http3());
        Assertions.assertEquals("/var/artipie/keystore.jks", stn.jksPath());
        Assertions.assertEquals("secret", stn.jksPwd());
        Assertions.assertEquals(stn.proxies().size(), 2);
        final ProxySettings proxy = stn.proxies().get(1);
        Assertions.assertEquals(URI.create("https://proxy1.com"), proxy.uri());
        Assertions.assertEquals("user_realm", proxy.basicRealm());
        Assertions.assertEquals("user_name", proxy.basicUser());
        Assertions.assertEquals("user_password", proxy.basicPwd());
    }
}
