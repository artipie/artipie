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
 * SSL test for PFX.
 * @since 0.26
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TestClassWithoutTestCases"})
//@checkstyle AbbreviationAsWordInNameCheck (1 line)
final class SSLPfxRestTest extends SSLBaseRestTest {
    /**
     * PFX-file with certificate.
     */
    private static final String CERT_PFX = "cert.pfx";

    @Override
    Optional<KeyStore> keyStore() throws IOException {
        this.save(
            new Key.From(SSLPfxRestTest.CERT_PFX),
            new TestResource(String.format("ssl/%s", SSLPfxRestTest.CERT_PFX)).asBytes()
        );
        return new YamlSettings(
            Yaml.createYamlInput(
                String.join(
                    "",
                    "meta:\n",
                    "  ssl:\n",
                    "    enabled: true\n",
                    "    pfx:\n",
                    "      path: cert.pfx\n",
                    "      password: secret\n"
                )
            ).readYamlMapping()
        ).keyStore();
    }
}
