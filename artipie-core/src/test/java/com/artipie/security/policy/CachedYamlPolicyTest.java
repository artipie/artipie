/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.security.policy;

import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.auth.AuthUser;
import com.artipie.security.perms.Action;
import com.artipie.security.perms.AdapterBasicPermission;
import com.artipie.security.perms.User;
import com.artipie.security.perms.UserPermissions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.nio.charset.StandardCharsets;
import java.security.PermissionCollection;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link CachedYamlPolicy} and {@link UserPermissions}.
 * @since 1.2
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
class CachedYamlPolicyTest {

    /**
     * Cache for usernames and {@link UserPermissions}.
     */
    private Cache<String, UserPermissions> cache;

    /**
     * Cache for usernames and user roles and individual permissions.
     */
    private Cache<String, User> user;

    /**
     * Cache for role name and role permissions.
     */
    private Cache<String, PermissionCollection> roles;

    /**
     * Storage to read users and roles yaml files from.
     */
    private BlockingStorage asto;

    @BeforeEach
    void init() {
        this.asto = new BlockingStorage(new InMemoryStorage());
        this.cache = CacheBuilder.newBuilder().build();
        this.user = CacheBuilder.newBuilder().build();
        this.roles = CacheBuilder.newBuilder().build();
    }

    @Test
    void aliceCanReadFromMavenWithJavaDevRole() {
        this.asto.save(new Key.From("users/alice.yml"), this.aliceConfig());
        this.asto.save(new Key.From("roles/java-dev.yaml"), this.javaDev());
        final CachedYamlPolicy policy = new CachedYamlPolicy(
            this.cache, this.user, this.roles, this.asto
        );
        MatcherAssert.assertThat(
            "Alice can read from maven repo",
            policy.getPermissions(new AuthUser("alice", "test"))
                .implies(new AdapterBasicPermission("maven-repo", Action.Standard.READ)),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Cache with UserPermissions has 1 item",
            this.cache.size(),
            new IsEqual<>(1L)
        );
        MatcherAssert.assertThat(
            "Cache with user has 1 item",
            this.user.size(),
            new IsEqual<>(1L)
        );
        MatcherAssert.assertThat(
            "Cache with role permissions has 2 item (default and `java-dev`)",
            this.roles.size(),
            new IsEqual<>(2L)
        );
        this.user.invalidateAll();
        MatcherAssert.assertThat(
            "Alice can read from maven repo",
            policy.getPermissions(new AuthUser("alice", "test"))
                .implies(new AdapterBasicPermission("maven-repo", Action.Standard.READ)),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Cache with UserPermissions has 1 item",
            this.cache.size(),
            new IsEqual<>(1L)
        );
        MatcherAssert.assertThat(
            "Cache with user roles and individual permissions has 0 items",
            this.user.size(),
            new IsEqual<>(0L)
        );
        MatcherAssert.assertThat(
            "Cache with role permissions has 2 item (default and `java-dev`)",
            this.roles.size(),
            new IsEqual<>(2L)
        );
    }

    @Test
    void aliceCanReadFromRpmRepoWithIndividualPerm() {
        this.asto.save(new Key.From("users/alice.yml"), this.aliceConfig());
        final CachedYamlPolicy policy = new CachedYamlPolicy(
            this.cache, this.user, this.roles, this.asto
        );
        MatcherAssert.assertThat(
            "Alice can read from rpm repo",
            policy.getPermissions(new AuthUser("alice", "test"))
                .implies(new AdapterBasicPermission("rpm-repo", Action.Standard.READ)),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Cache with UserPermissions has 1 item",
            this.cache.size(),
            new IsEqual<>(1L)
        );
        MatcherAssert.assertThat(
            "Cache with user individual permissions and roles has 1 item",
            this.user.size(),
            new IsEqual<>(1L)
        );
        MatcherAssert.assertThat(
            "Cache with role permissions is empty",
            this.roles.size(),
            new IsEqual<>(0L)
        );
    }

    @Test
    void aliceCanReadWithDevRoleAndThenWithIndividualPerms() {
        this.asto.save(new Key.From("users/alice.yml"), this.aliceConfig());
        this.asto.save(new Key.From("roles/java-dev.yaml"), this.javaDev());
        final CachedYamlPolicy policy = new CachedYamlPolicy(
            this.cache, this.user, this.roles, this.asto
        );
        MatcherAssert.assertThat(
            "Alice can read from maven repo",
            policy.getPermissions(new AuthUser("alice", "test"))
                .implies(new AdapterBasicPermission("maven-repo", Action.Standard.READ)),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Cache with UserPermissions has 1 item",
            this.cache.size(),
            new IsEqual<>(1L)
        );
        MatcherAssert.assertThat(
            "Cache with user individual permissions and roles has 1 item",
            this.user.size(),
            new IsEqual<>(1L)
        );
        MatcherAssert.assertThat(
            "Cache with role permissions has 2 items (default role and `java-def` role)",
            this.roles.size(),
            new IsEqual<>(2L)
        );
        MatcherAssert.assertThat(
            "Alice can read from rpm repo",
            policy.getPermissions(new AuthUser("alice", "test"))
                .implies(new AdapterBasicPermission("rpm-repo", Action.Standard.READ)),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Cache with UserPermissions has 1 item",
            this.cache.size(),
            new IsEqual<>(1L)
        );
        MatcherAssert.assertThat(
            "Cache with user individual permissions and roles has 1 item",
            this.user.size(),
            new IsEqual<>(1L)
        );
        MatcherAssert.assertThat(
            "Cache with role permissions has 2 items (default and `java-dev`)",
            this.roles.size(),
            new IsEqual<>(2L)
        );
    }

    @Test
    void anonymousCanReadWithDevRoleAndThenWithIndividualPerms() {
        this.asto.save(new Key.From("users/anonymous.yml"), this.anonymousConfig());
        this.asto.save(new Key.From("roles/java-dev.yaml"), this.javaDev());
        final CachedYamlPolicy policy = new CachedYamlPolicy(
            this.cache, this.user, this.roles, this.asto
        );
        MatcherAssert.assertThat(
            "Anonymous can read from maven repo",
            policy.getPermissions(AuthUser.ANONYMOUS)
                .implies(new AdapterBasicPermission("maven-repo", Action.Standard.READ)),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Cache with UserPermissions has 1 item",
            this.cache.size(),
            new IsEqual<>(1L)
        );
        MatcherAssert.assertThat(
            "Cache with user individual permissions and roles has 1 item",
            this.user.size(),
            new IsEqual<>(1L)
        );
        MatcherAssert.assertThat(
            "Cache with role permissions has 2 items (default role and `java-def` role)",
            this.roles.size(),
            new IsEqual<>(2L)
        );
        MatcherAssert.assertThat(
            "Anonymous can read from rpm repo",
            policy.getPermissions(AuthUser.ANONYMOUS)
                .implies(new AdapterBasicPermission("rpm-repo", Action.Standard.READ)),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Cache with UserPermissions has 1 item",
            this.cache.size(),
            new IsEqual<>(1L)
        );
        MatcherAssert.assertThat(
            "Cache with user individual permissions and roles has 1 item",
            this.user.size(),
            new IsEqual<>(1L)
        );
        MatcherAssert.assertThat(
            "Cache with role permissions has 2 items (default and `java-dev`)",
            this.roles.size(),
            new IsEqual<>(2L)
        );
    }

    @Test
    void johnCannotWriteIntoTestRepo() {
        this.asto.save(new Key.From("users/john.yml"), this.johnConfig());
        this.asto.save(new Key.From("roles/java-dev.yaml"), this.javaDev());
        this.asto.save(new Key.From("roles/tester.yaml"), this.tester());
        final CachedYamlPolicy policy = new CachedYamlPolicy(
            this.cache, this.user, this.roles, this.asto
        );
        MatcherAssert.assertThat(
            "John cannot write into test-repo",
            policy.getPermissions(new AuthUser("john", "env")).implies(
                new AdapterBasicPermission("test-repo", Action.Standard.WRITE)
            ),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat(
            "Cache with UserPermissions has 1 item",
            this.cache.size(),
            new IsEqual<>(1L)
        );
        MatcherAssert.assertThat(
            "Cache with user individual permissions and roles has 1 item",
            this.user.size(),
            new IsEqual<>(1L)
        );
        MatcherAssert.assertThat(
            "Cache with role permissions has 3 items (default role from context, `java-dev`, `tester`)",
            this.roles.size(),
            new IsEqual<>(3L)
        );
    }

    @Test
    void invalidatesGroupCaches() {
        this.asto.save(new Key.From("users/john.yml"), this.johnConfig());
        this.asto.save(new Key.From("roles/java-dev.yaml"), this.javaDev());
        this.asto.save(new Key.From("roles/tester.yaml"), this.tester());
        final CachedYamlPolicy policy = new CachedYamlPolicy(
            this.cache, this.user, this.roles, this.asto
        );
        MatcherAssert.assertThat(
            "John cannot write into test-repo",
            policy.getPermissions(new AuthUser("john", "env")).implies(
                new AdapterBasicPermission("test-repo", Action.Standard.WRITE)
            ),
            new IsEqual<>(false)
        );
        policy.invalidate("tester");
        policy.invalidate("default/env");
        MatcherAssert.assertThat(
            "Cache with UserPermissions has 1 item",
            this.cache.size(),
            new IsEqual<>(1L)
        );
        MatcherAssert.assertThat(
            "Cache with user individual permissions and roles has 1 item",
            this.user.size(),
            new IsEqual<>(1L)
        );
        MatcherAssert.assertThat(
            "Cache with role permissions has 'java-dev' group",
            this.roles.size() == 1L && this.roles.asMap().containsKey("java-dev")
        );
    }

    @Test
    void invalidatesUsersCache() {
        this.asto.save(new Key.From("users/alice.yml"), this.aliceConfig());
        this.asto.save(new Key.From("roles/java-dev.yaml"), this.javaDev());
        final CachedYamlPolicy policy = new CachedYamlPolicy(
            this.cache, this.user, this.roles, this.asto
        );
        MatcherAssert.assertThat(
            "Alice can read from maven repo",
            policy.getPermissions(new AuthUser("alice", "test"))
                .implies(new AdapterBasicPermission("maven-repo", Action.Standard.READ)),
            new IsEqual<>(true)
        );
        policy.invalidate("alice");
        MatcherAssert.assertThat(
            "Cache with UserPermissions has 1 item",
            this.cache.size(),
            new IsEqual<>(0L)
        );
        MatcherAssert.assertThat(
            "Cache with user individual permissions and roles is empty",
            this.user.size(),
            new IsEqual<>(0L)
        );
        MatcherAssert.assertThat(
            "Cache with role permissions has 2 items (default and `java-def` role)",
            this.roles.size(),
            new IsEqual<>(2L)
        );
    }

    @Test
    void anyRepoTest() {
        this.asto.save(new Key.From("users/bob.yml"), this.configForAnyRepo("\"*\""));
        this.asto.save(new Key.From("users/marat.yml"), this.configForAnyRepo("'*'"));
        this.asto.save(new Key.From("users/nataly.yml"), this.configForAnyRepo("\\*"));
        this.asto.save(new Key.From("users/anton.yml"), this.configForAnyRepo("*"));
        final CachedYamlPolicy policy = new CachedYamlPolicy(
            this.cache, this.user, this.roles, this.asto
        );
        MatcherAssert.assertThat(
            "Bob can read from maven repo",
            policy.getPermissions(new AuthUser("bob", "test"))
                .implies(new AdapterBasicPermission("maven-repo", Action.Standard.READ)),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Bob cannot write to maven repo",
            policy.getPermissions(new AuthUser("bob", "test"))
                .implies(new AdapterBasicPermission("maven-repo", Action.Standard.WRITE)),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat(
            "marat can read from maven repo",
            policy.getPermissions(new AuthUser("marat", "test"))
                .implies(new AdapterBasicPermission("maven-repo", Action.Standard.READ)),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "marat cannot write to maven repo",
            policy.getPermissions(new AuthUser("marat", "test"))
                .implies(new AdapterBasicPermission("maven-repo", Action.Standard.WRITE)),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat(
            "nataly can read from maven repo",
            policy.getPermissions(new AuthUser("nataly", "test"))
                .implies(new AdapterBasicPermission("maven-repo", Action.Standard.READ)),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "nataly cannot write to maven repo",
            policy.getPermissions(new AuthUser("nataly", "test"))
                .implies(new AdapterBasicPermission("maven-repo", Action.Standard.WRITE)),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat(
            "anton can read from maven repo",
            policy.getPermissions(new AuthUser("anton", "test"))
                .implies(new AdapterBasicPermission("maven-repo", Action.Standard.READ)),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "anton cannot write to maven repo",
            policy.getPermissions(new AuthUser("anton", "test"))
                .implies(new AdapterBasicPermission("maven-repo", Action.Standard.WRITE)),
            new IsEqual<>(false)
        );
    }

    @Test
    void adminCanDoAnything() {
        this.asto.save(new Key.From("users/admin.yml"), this.adminConfig());
        final CachedYamlPolicy policy = new CachedYamlPolicy(
            this.cache, this.user, this.roles, this.asto
        );
        MatcherAssert.assertThat(
            "Admin can read from maven repo",
            policy.getPermissions(new AuthUser("admin", "test"))
                .implies(new AdapterBasicPermission("maven-repo", Action.Standard.READ)),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Admin can do any operation",
            policy.getPermissions(new AuthUser("admin", "test"))
                .implies(new AdapterBasicPermission("*", Action.ALL)),
            new IsEqual<>(true)
        );
    }

    @Test
    void defaultRolesWorks() {
        this.asto.save(new Key.From("roles/default/test.yml"), this.tester());
        this.asto.save(new Key.From("roles/default/env.yaml"), this.envAuthConfig());
        this.asto.save(new Key.From("roles/default/github.yml"), this.githubAuthConfig());
        final CachedYamlPolicy policy = new CachedYamlPolicy(
            this.cache, this.user, this.roles, this.asto
        );
        MatcherAssert.assertThat(
            "John with `test` context can read from test-repo",
            policy.getPermissions(new AuthUser("john", "test")).implies(
                new AdapterBasicPermission("test-repo", Action.Standard.READ)
            ),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Alice with `test` context can read from test-repo",
            policy.getPermissions(new AuthUser("Alice", "test")).implies(
                new AdapterBasicPermission("test-repo", Action.Standard.READ)
            ),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Mark with `test` context cannot write into test-repo",
            policy.getPermissions(new AuthUser("Mark", "test")).implies(
                new AdapterBasicPermission("test-repo", Action.Standard.WRITE)
            ),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat(
            "Alex with `env` context can write to test-repo",
            policy.getPermissions(new AuthUser("alex", "env")).implies(
                new AdapterBasicPermission("test-repo", Action.Standard.WRITE)
            ),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Alex with `env` context can delete from any-repo",
            policy.getPermissions(new AuthUser("alex", "env")).implies(
                new AdapterBasicPermission("any-repo", Action.Standard.DELETE)
            ),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Olga with `github` context can write to maven-repo",
            policy.getPermissions(new AuthUser("olga", "github")).implies(
                new AdapterBasicPermission("maven-repo", Action.Standard.WRITE)
            ),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Pavel with `github` context can read from pypi-repo",
            policy.getPermissions(new AuthUser("pavel", "github")).implies(
                new AdapterBasicPermission("pypi-repo", Action.Standard.READ)
            ),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Pavel with `github` context cannot read from any-repo",
            policy.getPermissions(new AuthUser("pavel", "github")).implies(
                new AdapterBasicPermission("any-repo", Action.Standard.READ)
            ),
            new IsEqual<>(false)
        );
    }

    private byte[] anonymousConfig() {
        return String.join(
            "\n",
            "roles:",
            "  - java-dev",
            "permissions:",
            "  adapter_basic_permissions:",
            "    rpm-repo:",
            "      - read",
            "    binary-test-repo:",
            "      - read"
        ).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] aliceConfig() {
        return String.join(
            "\n",
            "type: plain",
            "pass: qwerty",
            "email: alice@example.com",
            "roles:",
            "  - java-dev",
            "permissions:",
            "  adapter_basic_permissions:",
            "    rpm-repo:",
            "      - read",
            "    binary-test-repo:",
            "      - read",
            "      - write"
        ).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] javaDev() {
        return String.join(
            "\n",
            "permissions:",
            "  adapter_basic_permissions:",
            "    python-repo:",
            "      - read",
            "    npm-repo:",
            "      - read",
            "    maven-repo:",
            "      - read",
            "      - write"
        ).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] johnConfig() {
        return String.join(
            "\n",
            "type: plain",
            "pass: qwerty",
            "email: alice@example.com",
            "roles:",
            "  - java-dev",
            "  - tester",
            "permissions:",
            "  adapter_basic_permissions:",
            "    repo1:",
            "      - r",
            "      - w",
            "      - d"
        ).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] tester() {
        return String.join(
            "\n",
            "permissions:",
            "  adapter_basic_permissions:",
            "    test-repo:",
            "      - download"
        ).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] configForAnyRepo(final String wildcard) {
        return String.join(
            "\n",
            "permissions:",
            "  adapter_basic_permissions:",
            String.format("    %s:", wildcard),
            "      - download"
        ).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] adminConfig() {
        return String.join(
            "\n",
            "type: plain",
            "pass: qwerty",
            "email: admin@example.com",
            "permissions:",
            "  all_permission: {}"
        ).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] envAuthConfig() {
        return String.join(
            "\n",
            "permissions:",
            "  all_permission: {}"
        ).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] githubAuthConfig() {
        return String.join(
            "\n",
            "permissions:",
            "  adapter_basic_permissions:",
            "    maven-repo:",
            "      - *",
            "    pypi-repo:",
            "      - *"
        ).getBytes(StandardCharsets.UTF_8);
    }
}
