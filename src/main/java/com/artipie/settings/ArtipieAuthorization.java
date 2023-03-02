/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings;

import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlNode;
import com.amihaiemil.eoyaml.YamlSequence;
import com.artipie.ArtipieException;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.factory.Config;
import com.artipie.asto.factory.StoragesLoader;
import com.artipie.auth.AuthFromEnv;
import com.artipie.auth.AuthFromKeycloak;
import com.artipie.auth.AuthFromStorage;
import com.artipie.auth.GithubAuth;
import com.artipie.http.auth.Authentication;
import com.artipie.security.policy.PoliciesLoader;
import com.artipie.security.policy.Policy;
import com.artipie.security.policy.YamlPolicyConfig;
import com.artipie.settings.cache.CachedUsers;
import com.jcabi.log.Logger;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.keycloak.authorization.client.Configuration;

/**
 * Artipie authorization: authentication and permissions policy.
 * @since 0.29
 */
public interface ArtipieAuthorization {

    /**
     * Instance of {@link CachedUsers} which implements
     * {@link Authentication} and {@link com.artipie.asto.misc.Cleanable}.
     * @return Cached users
     */
    CachedUsers authentication();

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
     * Artipie authorization from yaml settings.
     * @since 0.29
     */
    class FromYaml implements ArtipieAuthorization {

        /**
         * YAML node name `type` for credentials type.
         */
        private static final String NODE_TYPE = "type";

        /**
         * Yaml node policy.
         */
        private static final String NODE_POLICY = "policy";

        /**
         * Yaml node storage.
         */
        private static final String NODE_STORAGE = "storage";

        /**
         * Artipie policy and creds type name.
         */
        private static final String ARTIPIE = "artipie";

        /**
         * Yaml node credentials.
         */
        private static final String NODE_CREDENTIALS = "credentials";

        /**
         * Permissions policy instance.
         */
        private final Policy<?> plc;

        /**
         * Instance of {@link CachedUsers} which implements
         * {@link Authentication} and {@link com.artipie.asto.misc.Cleanable}.
         */
        private final CachedUsers auth;

        /**
         * Policy storage if `artipie` policy is used or empty.
         */
        private final Optional<Storage> asto;

        /**
         * Ctor.
         * @param settings Yaml settings
         */
        public FromYaml(final YamlMapping settings) {
            this.auth = FromYaml.initAuth(settings);
            this.plc = FromYaml.initPolicy(settings);
            this.asto = FromYaml.initPolicyStorage(settings);
        }

        @Override
        public CachedUsers authentication() {
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
         * Initialise authentication. If `credentials` section is absent or empty,
         * {@link AuthFromEnv} is used.
         * @param settings Yaml settings
         * @return Authentication
         */
        private static CachedUsers initAuth(final YamlMapping settings) {
            Authentication res;
            final YamlSequence creds = settings.yamlSequence(FromYaml.NODE_CREDENTIALS);
            if (creds == null || creds.isEmpty()) {
                Logger.info(
                    ArtipieAuthorization.class,
                    "Credentials yaml section is absent or empty, using AuthFromEnv()"
                );
                res = new AuthFromEnv();
            } else {
                final List<Authentication> auth = creds.values().stream().map(
                    node -> CredentialsType.valueOf(node.asMapping())
                ).map(type -> type.auth(settings)).toList();
                res = auth.get(0);
                for (final Authentication users : auth.subList(1, auth.size())) {
                    res = new Authentication.Joined(res, users);
                }
            }
            return new CachedUsers(res);
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

        /**
         * Policy storage if `artipie` policy is used or empty.
         * @param cfg Yaml config
         * @return Storage if configured
         */
        private static Optional<Storage> initPolicyStorage(final YamlMapping cfg) {
            Optional<Storage> res = Optional.empty();
            final YamlSequence credentials = cfg.yamlSequence(FromYaml.NODE_CREDENTIALS);
            final YamlMapping policy = cfg.yamlMapping(FromYaml.NODE_POLICY);
            if (credentials != null && !credentials.isEmpty()) {
                final Optional<YamlMapping> asto = credentials
                    .values().stream().map(YamlNode::asMapping)
                    .filter(node -> FromYaml.ARTIPIE.equals(node.string(FromYaml.NODE_TYPE)))
                    .findFirst().map(node -> node.yamlMapping(FromYaml.NODE_STORAGE));
                if (asto.isPresent()) {
                    res = Optional.of(
                        new StoragesLoader().newObject(
                            asto.get().string(FromYaml.NODE_TYPE),
                            new Config.YamlStorageConfig(asto.get())
                        )
                    );
                } else if (policy != null
                    && FromYaml.ARTIPIE.equals(policy.string(FromYaml.NODE_TYPE))
                    && policy.yamlMapping(FromYaml.NODE_STORAGE) != null) {
                    res = Optional.of(
                        new StoragesLoader().newObject(
                            policy.yamlMapping(FromYaml.NODE_STORAGE).string(FromYaml.NODE_TYPE),
                            new Config.YamlStorageConfig(
                                policy.yamlMapping(FromYaml.NODE_STORAGE)
                            )
                        )
                    );
                }
            }
            return res;
        }
    }

    /**
     * Credentials types.
     *
     * @since 0.22
     */
    enum CredentialsType {

        /**
         * Credentials type: artipie.
         */
        ARTIPIE(cfg -> {
            return FromYaml.initPolicyStorage(cfg).map(
                asto -> new AuthFromStorage(new BlockingStorage(asto))
            ).orElseThrow(
                () ->  new ArtipieException(
                    "Failed to create artipie auth, storage is not configured"
                )
            );
        }),

        /**
         * Credentials type: github.
         */
        GITHUB(cfg -> new GithubAuth()),

        /**
         * Credentials type: env.
         */
        ENV(cfg -> new AuthFromEnv()),

        /**
         * Credentials type: keycloak.
         */
        KEYCLOAK(cfg -> {
            final YamlMapping creds = cfg.yamlSequence(FromYaml.NODE_CREDENTIALS)
                .values().stream().map(YamlNode::asMapping)
                .filter(node -> "keycloak".equals(node.string(FromYaml.NODE_TYPE)))
                .findFirst().orElseThrow();
            return new AuthFromKeycloak(
                new Configuration(
                    creds.string("url"),
                    creds.string("realm"),
                    creds.string("client-id"),
                    Map.of("secret", creds.string("client-password")),
                    null
                )
            );
        });

        /**
         * Transform yaml to completion action with authentication.
         */
        private final Function<YamlMapping, Authentication> map;

        /**
         * Ctor.
         *
         * @param map Transform yaml to completion action with users.
         */
        CredentialsType(final Function<YamlMapping, Authentication> map) {
            this.map = map;
        }

        /**
         * Get {@code CredentialsType} that corresponds to type from credentials
         * yaml mapping or {@code ENV} if {@code yaml} is null.
         *
         * @param yaml Credentials yaml mapping.
         * @return CredentialsType.
         */
        static CredentialsType valueOf(final YamlMapping yaml) {
            CredentialsType res = ENV;
            if (yaml != null) {
                res = CredentialsType.valueOf(
                    yaml.string(FromYaml.NODE_TYPE).toUpperCase(Locale.getDefault())
                );
            }
            return res;
        }

        /**
         * Transform yaml to completion action with authentication.
         *
         * @param yaml Credentials yaml mapping.
         * @return Completion action with users.
         */
        Authentication auth(final YamlMapping yaml) {
            return this.map.apply(yaml);
        }
    }

}
