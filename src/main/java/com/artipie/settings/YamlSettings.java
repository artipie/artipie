/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings;

import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlNode;
import com.amihaiemil.eoyaml.YamlSequence;
import com.artipie.ArtipieException;
import com.artipie.api.ssl.KeyStore;
import com.artipie.api.ssl.KeyStoreFactory;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.SubStorage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.factory.Config;
import com.artipie.asto.factory.StoragesLoader;
import com.artipie.auth.AuthFromEnv;
import com.artipie.auth.AuthFromKeycloak;
import com.artipie.auth.AuthFromStorage;
import com.artipie.auth.GithubAuth;
import com.artipie.http.auth.Authentication;
import com.artipie.settings.cache.ArtipieCaches;
import com.artipie.settings.cache.CachedStorages;
import com.artipie.settings.cache.CachedUsers;
import com.jcabi.log.Logger;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.keycloak.authorization.client.Configuration;

/**
 * Settings built from YAML.
 *
 * @since 0.1
 * @checkstyle ReturnCountCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class YamlSettings implements Settings {

    /**
     * Yaml node credentials.
     */
    public static final String NODE_CREDENTIALS = "credentials";

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
     * YAML node name for `ssl` yaml section.
     */
    private static final String NODE_SSL = "ssl";

    /**
     * YAML file content.
     */
    private final YamlMapping content;

    /**
     * A set of caches for artipie settings.
     */
    private final ArtipieCaches acach;

    /**
     * Metrics context.
     */
    private final MetricsContext mctx;

    /**
     * Authentication and policy.
     */
    private final ArtipieSecurity security;

    /**
     * Ctor.
     * @param content YAML file content.
     */
    @SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
    public YamlSettings(final YamlMapping content) {
        this.content = content;
        final CachedUsers auth = YamlSettings.initAuth(this.meta());
        this.security = new ArtipieSecurity.FromYaml(
            this.meta(), auth, YamlSettings.initPolicyStorage(this.meta())
        );
        this.acach = new ArtipieCaches.All(auth, new CachedStorages());
        this.mctx = new MetricsContext(this.meta());
    }

    @Override
    public Storage configStorage() {
        return this.acach.storagesCache().storage(this);
    }

    @Override
    public ArtipieSecurity authz() {
        return this.security;
    }

    @Override
    public Layout layout() {
        return Layout.valueOf(this.meta().string("layout"));
    }

    @Override
    public YamlMapping meta() {
        return Optional.ofNullable(this.content.yamlMapping("meta"))
            .orElseThrow(
                () -> new IllegalStateException(
                    "Invalid settings: not empty `meta` section is expected"
                )
            );
    }

    @Override
    public Storage repoConfigsStorage() {
        return Optional.ofNullable(this.meta().string("repo_configs"))
            .<Storage>map(str -> new SubStorage(new Key.From(str), this.configStorage()))
            .orElse(this.configStorage());
    }

    @Override
    public Optional<KeyStore> keyStore() {
        return Optional.ofNullable(this.meta().yamlMapping(YamlSettings.NODE_SSL))
            .map(KeyStoreFactory::newInstance);
    }

    @Override
    public MetricsContext metrics() {
        return this.mctx;
    }

    @Override
    public ArtipieCaches caches() {
        return this.acach;
    }

    @Override
    public String toString() {
        return String.format("YamlSettings{\n%s\n}", this.content.toString());
    }

    /**
     * Policy storage if `artipie` policy is used or empty.
     * @param cfg Yaml config
     * @return Storage if configured
     */
    private static Optional<Storage> initPolicyStorage(final YamlMapping cfg) {
        Optional<Storage> res = Optional.empty();
        final YamlSequence credentials = cfg.yamlSequence(YamlSettings.NODE_CREDENTIALS);
        final YamlMapping policy = cfg.yamlMapping(YamlSettings.NODE_POLICY);
        if (credentials != null && !credentials.isEmpty()) {
            final Optional<YamlMapping> asto = credentials
                .values().stream().map(YamlNode::asMapping)
                .filter(node -> YamlSettings.ARTIPIE.equals(node.string(YamlSettings.NODE_TYPE)))
                .findFirst().map(node -> node.yamlMapping(YamlSettings.NODE_STORAGE));
            if (asto.isPresent()) {
                res = Optional.of(
                    new StoragesLoader().newObject(
                        asto.get().string(YamlSettings.NODE_TYPE),
                        new Config.YamlStorageConfig(asto.get())
                    )
                );
            } else if (policy != null
                && YamlSettings.ARTIPIE.equals(policy.string(YamlSettings.NODE_TYPE))
                && policy.yamlMapping(YamlSettings.NODE_STORAGE) != null) {
                res = Optional.of(
                    new StoragesLoader().newObject(
                        policy.yamlMapping(YamlSettings.NODE_STORAGE)
                            .string(YamlSettings.NODE_TYPE),
                        new Config.YamlStorageConfig(
                            policy.yamlMapping(YamlSettings.NODE_STORAGE)
                        )
                    )
                );
            }
        }
        return res;
    }

    /**
     * Initialise authentication. If `credentials` section is absent or empty,
     * {@link AuthFromEnv} is used.
     * @param settings Yaml settings
     * @return Authentication
     */
    private static CachedUsers initAuth(final YamlMapping settings) {
        Authentication res;
        final YamlSequence creds = settings.yamlSequence(YamlSettings.NODE_CREDENTIALS);
        if (creds == null || creds.isEmpty()) {
            Logger.info(
                ArtipieSecurity.class,
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
     * Credentials types.
     *
     * @since 0.22
     */
    enum CredentialsType {

        /**
         * Credentials type: artipie.
         */
        ARTIPIE(cfg -> {
            return YamlSettings.initPolicyStorage(cfg).map(
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
            final YamlMapping creds = cfg.yamlSequence(YamlSettings.NODE_CREDENTIALS)
                .values().stream().map(YamlNode::asMapping)
                .filter(node -> "keycloak".equals(node.string(YamlSettings.NODE_TYPE)))
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
                    yaml.string(YamlSettings.NODE_TYPE).toUpperCase(Locale.getDefault())
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
