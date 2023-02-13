/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.api.ssl.KeyStore;
import com.artipie.asto.Key;
import com.artipie.asto.test.TestResource;
import com.artipie.settings.YamlSettings;
import java.io.IOException;
import java.util.Optional;

/**
 * SSL test for PEM.
 * @since 0.26
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TestClassWithoutTestCases"})
//@checkstyle AbbreviationAsWordInNameCheck (1 line)
final class SSLPemRestTest extends SSLBaseRestTest {
    /**
     * PEM-file with private key.
     */
    private static final String PRIVATE_KEY_PEM = "private-key.pem";

    /**
     * PEM-file with certificate.
     */
    private static final String CERT_PEM = "cert.pem";

    @Override
    Optional<KeyStore> keyStore() throws IOException {
        this.save(
            new Key.From(SSLPemRestTest.PRIVATE_KEY_PEM),
            new TestResource(String.format("ssl/%s", SSLPemRestTest.PRIVATE_KEY_PEM)).asBytes()
        );
        this.save(
            new Key.From(SSLPemRestTest.CERT_PEM),
            new TestResource(String.format("ssl/%s", SSLPemRestTest.CERT_PEM)).asBytes()
        );
        return new YamlSettings(
            Yaml.createYamlInput(
                String.join(
                    "",
                    "meta:\n",
                    "  ssl:\n",
                    "    enabled: true\n",
                    "    pem:\n",
                    "      key-path: private-key.pem\n",
                    "      cert-path: cert.pem\n"
                )
            ).readYamlMapping()
        ).keyStore();
    }
}
