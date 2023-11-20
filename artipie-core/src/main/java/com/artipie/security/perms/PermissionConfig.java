/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.security.perms;

import com.amihaiemil.eoyaml.Node;
import com.amihaiemil.eoyaml.Scalar;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlNode;
import com.amihaiemil.eoyaml.YamlSequence;
import com.artipie.asto.factory.Config;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Permission configuration.
 * @since 1.2
 */
public interface PermissionConfig extends Config {

    /**
     * Gets sequence of keys.
     *
     * @return Keys sequence.
     */
    Set<String> keys();

    /**
     * Yaml permission config.
     * Implementation note:
     * Yaml permission config allows {@link AdapterBasicPermission#WILDCARD} yaml sequence.In
     * yamls `*` sign can be quoted. Thus, we need to handle various quotes properly.
     * @since 1.2
     */
    final class FromYamlMapping implements PermissionConfig {

        /**
         * Yaml mapping to read permission from.
         */
        private final YamlMapping yaml;

        /**
         * Ctor.
         * @param yaml Yaml mapping to read permission from
         */
        public FromYamlMapping(final YamlMapping yaml) {
            this.yaml = yaml;
        }

        @Override
        public String string(final String key) {
            return this.yaml.string(key);
        }

        @Override
        public Set<String> sequence(final String key) {
            final Set<String> res;
            if (AdapterBasicPermission.WILDCARD.equals(key)) {
                res = this.yaml.yamlSequence(this.getWildcardKey(key)).values().stream()
                    .map(item -> item.asScalar().value()).collect(Collectors.toSet());
            } else {
                res = this.yaml.yamlSequence(key).values().stream().map(
                    item -> item.asScalar().value()
                ).collect(Collectors.toSet());
            }
            return res;
        }

        @Override
        public Set<String> keys() {
            return this.yaml.keys().stream().map(node -> node.asScalar().value())
                .map(FromYamlMapping::cleanName).collect(Collectors.toSet());
        }

        @Override
        public PermissionConfig config(final String key) {
            final PermissionConfig res;
            if (AdapterBasicPermission.WILDCARD.equals(key)) {
                res = FromYamlMapping.configByNode(this.yaml.value(this.getWildcardKey(key)));
            } else {
                res = FromYamlMapping.configByNode(this.yaml.value(key));
            }
            return res;
        }

        @Override
        public boolean isEmpty() {
            return this.yaml == null || this.yaml.isEmpty();
        }

        /**
         * Find wildcard key as it can be escaped in various ways.
         * @param key The key
         * @return Escaped key to get sequence or mapping with it
         */
        private Scalar getWildcardKey(final String key) {
            return this.yaml.keys().stream().map(YamlNode::asScalar).filter(
                item -> item.value().contains(AdapterBasicPermission.WILDCARD)
            ).findFirst().orElseThrow(
                () -> new IllegalStateException(
                    String.format("Sequence %s not found", key)
                )
            );
        }

        /**
         * Cleans wildcard value from various escape signs.
         * @param value Value to check and clean
         * @return Cleaned value
         */
        private static String cleanName(final String value) {
            String res = value;
            if (value.contains(AdapterBasicPermission.WILDCARD)) {
                res = value.replace("\"", "").replace("'", "").replace("\\", "");
            }
            return res;
        }

        /**
         * Config by yaml node with respect to this node type.
         * @param node Yaml node to create config from
         * @return Sub-config
         */
        private static PermissionConfig configByNode(final YamlNode node) {
            final PermissionConfig res;
            if (node.type() == Node.MAPPING) {
                res = new FromYamlMapping(node.asMapping());
            } else if (node.type() == Node.SEQUENCE) {
                res = new FromYamlSequence(node.asSequence());
            } else {
                throw new IllegalArgumentException("Yaml sub-config not found!");
            }
            return res;
        }
    }

    /**
     * Permission config from yaml sequence. In this implementation, string parameter represents
     * sequence index, thus integer value is expected. Method {@link FromYamlSequence#keys()}
     * returns the sequence as a set of strings.
     * @since 1.3
     */
    final class FromYamlSequence implements PermissionConfig {

        /**
         * Yaml sequence.
         */
        private final YamlSequence seq;

        /**
         * Ctor.
         * @param seq Sequence
         */
        public FromYamlSequence(final YamlSequence seq) {
            this.seq = seq;
        }

        @Override
        public Set<String> keys() {
            return this.seq.values().stream().map(YamlNode::asScalar).map(Scalar::value)
                .collect(Collectors.toSet());
        }

        @Override
        public String string(final String index) {
            return this.seq.string(Integer.parseInt(index));
        }

        @Override
        public Collection<String> sequence(final String index) {
            return this.seq.yamlSequence(Integer.parseInt(index)).values().stream()
                .map(YamlNode::asScalar).map(Scalar::value).collect(Collectors.toSet());
        }

        @Override
        @SuppressWarnings("PMD.ConfusingTernary")
        public PermissionConfig config(final String index) {
            final int ind = Integer.parseInt(index);
            final PermissionConfig res;
            if (this.seq.yamlSequence(ind) != null) {
                res = new FromYamlSequence(this.seq.yamlSequence(ind));
            } else if (this.seq.yamlMapping(ind) != null) {
                res = new FromYamlMapping(this.seq.yamlMapping(ind));
            } else {
                throw new IllegalArgumentException(
                    String.format("Sub config by index %s not found", index)
                );
            }
            return res;
        }

        @Override
        public boolean isEmpty() {
            return this.seq == null || this.seq.isEmpty();
        }
    }
}
