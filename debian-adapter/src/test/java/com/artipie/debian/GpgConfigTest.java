/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.debian;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.Content;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.slice.KeyFromPath;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link GpgConfig.FromYaml}.
 * @since 0.4
 */
class GpgConfigTest {

    @Test
    void returnsPassword() {
        final String pswd = "123";
        MatcherAssert.assertThat(
            new GpgConfig.FromYaml(
                Yaml.createYamlMappingBuilder()
                    .add(GpgConfig.FromYaml.GPG_PASSWORD, pswd).build(),
                new InMemoryStorage()
            ).password(),
            new IsEqual<>(pswd)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"/one/two/my_key.gpg", "one/some_key.gpg", "key.gpg", "/secret.gpg"})
    void returnsKey(final String key) {
        final byte[] bytes = "abc".getBytes();
        final InMemoryStorage storage = new InMemoryStorage();
        storage.save(new KeyFromPath(key), new Content.From(bytes)).join();
        MatcherAssert.assertThat(
            new GpgConfig.FromYaml(
                Yaml.createYamlMappingBuilder()
                    .add(GpgConfig.FromYaml.GPG_SECRET_KEY, key).build(),
                storage
            ).key().toCompletableFuture().join(),
            new IsEqual<>(bytes)
        );
    }
}
