/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Storage;
import com.artipie.http.auth.Authentication;
import com.artipie.security.policy.PoliciesLoader;
import com.artipie.security.policy.Policy;
import com.artipie.security.policy.YamlPolicyConfig;
import com.artipie.settings.cache.CachedUsers;
import java.util.Optional;

/**
 * Artipie security: authentication and permissions policy.
 * @since 0.29
 */
public interface ArtipieSecurity {

    /**
     * Instance of {@link CachedUsers} which implements
     * {@link Authentication} and {@link com.artipie.asto.misc.Cleanable}.
     * @return Cached users
     */
    Authentication authentication();

    /**
     * Permissions policy instance.
     * @return Policy
     */
    Policy<?> policy();

    /**
     * Policy storage if `artipie` policy is used or empty.
     * @return Storage for `artipie` policy
     */
    Optional<Storage> policyStorage();

    /**
     * Artipie security from yaml settings.
     * @since 0.29
     */
    class FromYaml implements ArtipieSecurity {

        /**
         * YAML node name `type` for credentials type.
         */
        private static final String NODE_TYPE = "type";

        /**
         * Yaml node policy.
         */
        private static final String NODE_POLICY = "policy";

        /**
         * Permissions policy instance.
         */
        private final Policy<?> plc;

        /**
         * Instance of {@link CachedUsers} which implements
         * {@link Authentication} and {@link com.artipie.asto.misc.Cleanable}.
         */
        private final Authentication auth;

        /**
         * Policy storage if `artipie` policy is used or empty.
         */
        private final Optional<Storage> asto;

        /**
         * Ctor.
         * @param settings Yaml settings
         * @param auth Authentication instance
         * @param asto Policy storage
         */
        public FromYaml(final YamlMapping settings, final Authentication auth,
            final Optional<Storage> asto) {
            this.auth = auth;
            this.plc = FromYaml.initPolicy(settings);
            this.asto = asto;
        }

        @Override
        public Authentication authentication() {
            return this.auth;
        }

        @Override
        public Policy<?> policy() {
            return this.plc;
        }

        @Override
        public Optional<Storage> policyStorage() {
            return this.asto;
        }

        /**
         * Initialize policy. If policy section is absent, {@link Policy#FREE} is used.
         * @param settings Yaml settings
         * @return Policy instance
         */
        private static Policy<?> initPolicy(final YamlMapping settings) {
            final YamlMapping mapping = settings.yamlMapping(FromYaml.NODE_POLICY);
            final Policy<?> res;
            if (mapping == null) {
                res = Policy.FREE;
            } else {
                res = new PoliciesLoader().newObject(
                    mapping.string(FromYaml.NODE_TYPE), new YamlPolicyConfig(mapping)
                );
            }
            return res;
        }

    }

}
