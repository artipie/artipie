/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.security.policy;

import com.amihaiemil.eoyaml.Yaml;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link YamlPolicyFactory}.
 * @since 1.2
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class YamlPolicyFactoryTest {

    @Test
    void createsYamlPolicy() {
        MatcherAssert.assertThat(
            new YamlPolicyFactory().getPolicy(
                new YamlPolicyConfig(
                    Yaml.createYamlMappingBuilder().add("type", "artipie")
                        .add(
                            "storage",
                            Yaml.createYamlMappingBuilder().add("type", "fs")
                                .add("path", "/some/path").build()
                        ).build()
                )
            ),
            new IsInstanceOf(CachedYamlPolicy.class)
        );
    }

    @Test
    void createsYamlPolicyWithEviction() {
        MatcherAssert.assertThat(
            new YamlPolicyFactory().getPolicy(
                new YamlPolicyConfig(
                    Yaml.createYamlMappingBuilder().add("type", "artipie")
                        .add("eviction_millis", "50000")
                        .add(
                            "storage",
                            Yaml.createYamlMappingBuilder().add("type", "fs")
                                .add("path", "/some/path").build()
                        ).build()
                )
            ),
            new IsInstanceOf(CachedYamlPolicy.class)
        );
    }

}
