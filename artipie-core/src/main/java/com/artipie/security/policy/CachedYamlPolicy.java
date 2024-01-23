/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.security.policy;

import com.amihaiemil.eoyaml.Node;
import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlNode;
import com.amihaiemil.eoyaml.YamlSequence;
import com.artipie.ArtipieException;
import com.artipie.asto.Key;
import com.artipie.asto.ValueNotFoundException;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.misc.Cleanable;
import com.artipie.asto.misc.UncheckedFunc;
import com.artipie.asto.misc.UncheckedSupplier;
import com.artipie.http.auth.AuthUser;
import com.artipie.security.perms.EmptyPermissions;
import com.artipie.security.perms.PermissionConfig;
import com.artipie.security.perms.PermissionsLoader;
import com.artipie.security.perms.User;
import com.artipie.security.perms.UserPermissions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jcabi.log.Logger;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Cached yaml policy implementation obtains permissions from yaml files and uses
 * {@link Cache} cache to avoid reading yamls from storage on each request.
 * <p/>
 * The storage itself is expected to have yaml files with permissions in the following structure:
 * <pre>
 * ..
 * ├── roles
 * │   ├── java-dev.yaml
 * │   ├── admin.yaml
 * │   ├── ...
 * ├── users
 * │   ├── david.yaml
 * │   ├── jane.yaml
 * │   ├── ...
 * </pre>
 * Roles yaml file name is the name of the role, format example for `java-dev.yaml`:
 * <pre>{@code
 * permissions:
 *   adapter_basic_permissions:
 *     maven-repo:
 *       - read
 *       - write
 *     python-repo:
 *       - read
 *     npm-repo:
 *       - read
 * }</pre>
 * Or for `admin.yaml`:
 * <pre>{@code
 * enabled: true # optional default true
 * permissions:
 *   all_permission: {}
 * }</pre>
 * Role can be disabled with the help of optional {@code enabled} field.
 * <p>User yaml format example, file name is the name of the user:
 * <pre>{@code
 * type: plain
 * pass: qwerty
 * email: david@example.com # Optional
 * enabled: true # optional default true
 * roles:
 *   - java-dev
 * permissions:
 *   artipie_basic_permission:
 *     rpm-repo:
 *       - read
 * }</pre>
 * @since 1.2
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class CachedYamlPolicy implements Policy<UserPermissions>, Cleanable<String> {

    /**
     * Permissions factories.
     */
    private static final PermissionsLoader FACTORIES = new PermissionsLoader();

    /**
     * Empty permissions' config.
     */
    private static final PermissionConfig EMPTY_CONFIG =
        new PermissionConfig.FromYamlMapping(Yaml.createYamlMappingBuilder().build());

    /**
     * Cache for usernames and {@link UserPermissions}.
     */
    private final Cache<String, UserPermissions> cache;

    /**
     * Cache for usernames and user with his roles and individual permissions.
     */
    private final Cache<String, User> users;

    /**
     * Cache for role name and role permissions.
     */
    private final Cache<String, PermissionCollection> roles;

    /**
     * Storage to read users and roles yaml files from.
     */
    private final BlockingStorage asto;

    /**
     * Primary ctor.
     * @param cache Cache for usernames and {@link UserPermissions}
     * @param users Cache for username and user individual permissions
     * @param roles Cache for role name and role permissions
     * @param asto Storage to read users and roles yaml files from
         */
    CachedYamlPolicy(
        final Cache<String, UserPermissions> cache,
        final Cache<String, User> users,
        final Cache<String, PermissionCollection> roles,
        final BlockingStorage asto
    ) {
        this.cache = cache;
        this.users = users;
        this.roles = roles;
        this.asto = asto;
    }

    /**
     * Ctor.
     * @param asto Storage to read users and roles yaml files from
     * @param eviction Eviction time in seconds
     */
    public CachedYamlPolicy(final BlockingStorage asto, final long eviction) {
        this(
            CacheBuilder.newBuilder().expireAfterAccess(eviction, TimeUnit.MILLISECONDS).build(),
            CacheBuilder.newBuilder().expireAfterAccess(eviction, TimeUnit.MILLISECONDS).build(),
            CacheBuilder.newBuilder().expireAfterAccess(eviction, TimeUnit.MILLISECONDS).build(),
            asto
        );
    }

    @Override
    public UserPermissions getPermissions(final AuthUser user) {
        try {
            return this.cache.get(user.name(), this.createUserPermissions(user));
        } catch (final ExecutionException err) {
            Logger.error("security", err.getMessage());
            throw new ArtipieException(err);
        }
    }

    @Override
    public void invalidate(final String key) {
        if (this.cache.asMap().containsKey(key)) {
            this.cache.invalidate(key);
            this.users.invalidate(key);
        } else if (this.roles.asMap().containsKey(key)) {
            this.roles.invalidate(key);
        }
    }

    @Override
    public void invalidateAll() {
        this.cache.invalidateAll();
        this.users.invalidateAll();
        this.roles.invalidateAll();
    }

    /**
     * Get role permissions.
     * @param asto Storage to read the role permissions from
     * @param role Role name
     * @return Permissions of the role
     */
    static PermissionCollection rolePermissions(final BlockingStorage asto, final String role) {
        PermissionCollection res;
        final String filename = String.format("roles/%s", role);
        try {
            final YamlMapping mapping = CachedYamlPolicy.readFile(asto, filename);
            final String enabled = mapping.string(AstoUser.ENABLED);
            if (Boolean.FALSE.toString().equalsIgnoreCase(enabled)) {
                res = EmptyPermissions.INSTANCE;
            } else {
                res = CachedYamlPolicy.readPermissionsFromYaml(mapping);
            }
        } catch (final IOException | ValueNotFoundException err) {
            Logger.error("security", String.format("Failed to read/parse file '%s'", filename));
            res = EmptyPermissions.INSTANCE;
        }
        return res;
    }

    /**
     * Create instance for {@link UserPermissions} if not found in cache,
     * arguments for the {@link UserPermissions} ctor are the following:
     * 1) supplier for user individual permissions and roles
     * 2) function to get permissions of the role.
     * @param user Username
     * @return Callable to create {@link UserPermissions}
         */
    private Callable<UserPermissions> createUserPermissions(final AuthUser user) {
        return () -> new UserPermissions(
            new UncheckedSupplier<>(
                () -> this.users.get(user.name(), () -> new AstoUser(this.asto, user))
            ),
            new UncheckedFunc<>(
                role -> this.roles.get(
                    role, () -> CachedYamlPolicy.rolePermissions(this.asto, role)
                )
            )
        );
    }

    /**
     * Read yaml file from storage considering both yaml and yml extensions. If nighter
     * version exists, exception is thrown.
     * @param asto Blocking storage
     * @param filename The name of the file
     * @return The value in bytes
     * @throws ValueNotFoundException If file not found
     * @throws IOException If yaml parsing failed
     */
    private static YamlMapping readFile(final BlockingStorage asto, final String filename)
        throws IOException {
        final byte[] res;
        final Key yaml = new Key.From(String.format("%s.yaml", filename));
        final Key yml = new Key.From(String.format("%s.yml", filename));
        if (asto.exists(yaml)) {
            res = asto.value(yaml);
        } else if (asto.exists(yml)) {
            res = asto.value(yml);
        } else {
            throw new ValueNotFoundException(yaml);
        }
        return Yaml.createYamlInput(new ByteArrayInputStream(res)).readYamlMapping();
    }

    /**
     * Read and instantiate permissions from yaml mapping.
     * @param mapping Yaml mapping
     * @return Permissions set
     */
    private static PermissionCollection readPermissionsFromYaml(final YamlMapping mapping) {
        final YamlMapping all = mapping.yamlMapping("permissions");
        final PermissionCollection res;
        if (all == null || all.keys().isEmpty()) {
            res = EmptyPermissions.INSTANCE;
        } else {
            res = new Permissions();
            for (final String type : all.keys().stream().map(item -> item.asScalar().value())
                .collect(Collectors.toSet())) {
                final YamlNode perms = all.value(type);
                final PermissionConfig config;
                if (perms != null && perms.type() == Node.MAPPING) {
                    config = new PermissionConfig.FromYamlMapping(perms.asMapping());
                } else if (perms != null && perms.type() == Node.SEQUENCE) {
                    config = new PermissionConfig.FromYamlSequence(perms.asSequence());
                } else {
                    config = CachedYamlPolicy.EMPTY_CONFIG;
                }
                Collections.list(FACTORIES.newObject(type, config).elements()).forEach(res::add);
            }
        }
        return res;
    }

    /**
     * User from storage.
     * @since 1.2
     */
    @SuppressWarnings({
        "PMD.AvoidFieldNameMatchingMethodName",
        "PMD.ConstructorOnlyInitializesOrCallOtherConstructors"
    })
    public static final class AstoUser implements User {

        /**
         * String to format user settings file name.
         */
        private static final String ENABLED = "enabled";

        /**
         * String to format user settings file name.
         */
        private static final String FORMAT = "users/%s";

        /**
         * User individual permission.
         */
        private final PermissionCollection perms;

        /**
         * User roles.
         */
        private final Collection<String> roles;

        /**
         * Ctor.
         * @param asto Storage to read user yaml file from
         * @param user The name of the user
         */
        AstoUser(final BlockingStorage asto, final AuthUser user) {
            final YamlMapping yaml = getYamlMapping(asto, user.name());
            this.perms = perms(yaml);
            this.roles = roles(yaml, user);
        }

        @Override
        public PermissionCollection perms() {
            return this.perms;
        }

        @Override
        public Collection<String> roles() {
            return this.roles;
        }

        /**
         * Get supplier to read user permissions from storage.
         * @param yaml Yaml to read permissions from
         * @return User permissions supplier
         */
        private static PermissionCollection perms(final YamlMapping yaml) {
            final PermissionCollection res;
            if (AstoUser.disabled(yaml)) {
                res = EmptyPermissions.INSTANCE;
            } else {
                res = CachedYamlPolicy.readPermissionsFromYaml(yaml);
            }
            return res;
        }

        /**
         * Get user roles collection.
         * @param yaml Yaml to read roles from
         * @param user Authenticated user
         * @return Roles collection
         */
        private static Collection<String> roles(final YamlMapping yaml, final AuthUser user) {
            Set<String> roles = Collections.emptySet();
            if (!AstoUser.disabled(yaml)) {
                final YamlSequence sequence = yaml.yamlSequence("roles");
                if (sequence != null) {
                    roles = sequence.values().stream().map(item -> item.asScalar().value())
                        .collect(Collectors.toSet());
                }
                if (user.authContext() != null && !user.authContext().isEmpty()) {
                    final String role = String.format("default/%s", user.authContext());
                    if (roles.isEmpty()) {
                        roles = Collections.singleton(role);
                    } else {
                        roles.add(role);
                    }
                }
            }
            return roles;
        }

        /**
         * Is user enabled?
         * @param yaml Yaml to check disabled item from
         * @return True is user is active
         */
        private static boolean disabled(final YamlMapping yaml) {
            return Boolean.FALSE.toString().equalsIgnoreCase(yaml.string(AstoUser.ENABLED));
        }

        /**
         * Read yaml mapping properly handling the possible errors.
         * @param asto Storage to read user yaml file from
         * @param username The name of the user
         * @return Yaml mapping
         */
        private static YamlMapping getYamlMapping(final BlockingStorage asto,
            final String username) {
            final String filename = String.format(AstoUser.FORMAT, username);
            YamlMapping res;
            try {
                res = CachedYamlPolicy.readFile(asto, filename);
            } catch (final IOException | ValueNotFoundException err) {
                Logger.error("security", "Failed to read or parse file '%s'", filename);
                res = Yaml.createYamlMappingBuilder().build();
            }
            return res;
        }
    }
}
