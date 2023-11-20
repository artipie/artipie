/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.security.policy;

import com.amihaiemil.eoyaml.Yaml;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link YamlPolicyConfig}.
 * @since 1.2
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class PolicyConfigYamlTest {

    @Test
    void readsStringValue() {
        MatcherAssert.assertThat(
            new YamlPolicyConfig(Yaml.createYamlMappingBuilder().add("key", "value").build())
                .string("key"),
            new IsEqual<>("value")
        );
    }

    @Test
    void readsSequence() {
        MatcherAssert.assertThat(
            new YamlPolicyConfig(
                Yaml.createYamlMappingBuilder()
                    .add("key", Yaml.createYamlSequenceBuilder().add("one").add("two").build())
                    .build()
            ).sequence("key"),
            Matchers.contains("one", "two")
        );
    }

    @Test
    void returnsEmptySequenceWhenAbsent() {
        MatcherAssert.assertThat(
            new YamlPolicyConfig(Yaml.createYamlMappingBuilder().build()).sequence("key"),
            new IsEmptyCollection<>()
        );
    }

    @Test
    void readsSubConfig() {
        MatcherAssert.assertThat(
            new YamlPolicyConfig(
                Yaml.createYamlMappingBuilder().add(
                    "key", Yaml.createYamlMappingBuilder().add("sub_key", "sub_value").build()
                ).build()
            ).config("key"),
            new IsInstanceOf(YamlPolicyConfig.class)
        );
    }

}
