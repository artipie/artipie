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
 * SSL test for JKS.
 * @since 0.26
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TestClassWithoutTestCases"})
//@checkstyle AbbreviationAsWordInNameCheck (1 line)
final class SSLJksRestTest extends SSLBaseRestTest {
    /**
     * JKS-file.
     */
    private static final String JKS = "keystore.jks";

    @Override
    Optional<KeyStore> keyStore() throws IOException {
        this.save(
            new Key.From(SSLJksRestTest.JKS),
            new TestResource(String.format("ssl/%s", SSLJksRestTest.JKS)).asBytes()
        );
        return new YamlSettings(
            Yaml.createYamlInput(
                String.join(
                    "",
                    "meta:\n",
                    "  ssl:\n",
                    "    enabled: true\n",
                    "    jks:\n",
                    "      path: keystore.jks\n",
                    "      password: secret"
                )
            ).readYamlMapping()
        ).keyStore();
    }
}
