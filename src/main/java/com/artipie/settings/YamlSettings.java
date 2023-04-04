/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings;

import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlNode;
import com.amihaiemil.eoyaml.YamlSequence;
import com.artipie.api.ssl.KeyStore;
import com.artipie.api.ssl.KeyStoreFactory;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.SubStorage;
import com.artipie.asto.factory.Config;
import com.artipie.asto.factory.StoragesLoader;
import com.artipie.auth.AuthFromEnv;
import com.artipie.http.auth.AuthLoader;
import com.artipie.http.auth.Authentication;
import com.artipie.settings.cache.ArtipieCaches;
import com.artipie.settings.cache.CachedStorages;
import com.artipie.settings.cache.CachedUsers;
import com.jcabi.log.Logger;
import java.util.List;
import java.util.Optional;

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
    public static final String NODE_TYPE = "type";

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
            this.meta(), auth, new PolicyStorage(this.meta()).parse()
        );
        this.acach = new ArtipieCaches.All(auth, new CachedStorages(), this.security.policy());
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
            final AuthLoader loader = new AuthLoader();
            final List<Authentication> auth = creds.values().stream().map(
                node -> node.asMapping().string(YamlSettings.NODE_TYPE)
            ).map(type -> loader.newObject(type, settings)).toList();
            res = auth.get(0);
            for (final Authentication users : auth.subList(1, auth.size())) {
                res = new Authentication.Joined(res, users);
            }
        }
        return new CachedUsers(res);
    }

    /**
     * Policy (auth and permissions) storage from config yaml.
     * @since 0.13
     */
    public static class PolicyStorage {

        /**
         * Yaml mapping config.
         */
        private final YamlMapping cfg;

        /**
         * Ctor.
         * @param cfg Settings config
         */
        public PolicyStorage(final YamlMapping cfg) {
            this.cfg = cfg;
        }

        /**
         * Read policy storage from config yaml. Normally policy storage should be configured
         * in `policy` yaml section, but, if policy is absent, storage should be specified in
         * credentials sections for `artipie` credentials type.
         * @return Storage if present
         */
        public Optional<Storage> parse() {
            Optional<Storage> res = Optional.empty();
            final YamlSequence credentials = this.cfg.yamlSequence(YamlSettings.NODE_CREDENTIALS);
            final YamlMapping policy = this.cfg.yamlMapping(YamlSettings.NODE_POLICY);
            if (credentials != null && !credentials.isEmpty()) {
                final Optional<YamlMapping> asto = credentials
                    .values().stream().map(YamlNode::asMapping)
                    .filter(
                        node -> YamlSettings.ARTIPIE.equals(node.string(YamlSettings.NODE_TYPE))
                    ).findFirst().map(node -> node.yamlMapping(YamlSettings.NODE_STORAGE));
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
    }

}
