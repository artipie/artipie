/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link RepoPermissions.FromSettings}.
 * @since 0.10
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class RepoPermissionsFromSettingsTest {

    /**
     * Storage.
     */
    private Storage storage;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
    }

    @Test
    void returnsRepoList() {
        this.storage.save(new Key.From("one.yaml"), Content.EMPTY).join();
        this.storage.save(new Key.From("two.yaml"), Content.EMPTY).join();
        this.storage.save(new Key.From("abc"), Content.EMPTY).join();
        this.storage.save(new Key.From("three.yaml"), Content.EMPTY).join();
        MatcherAssert.assertThat(
            new RepoPermissions.FromSettings(new Settings.Fake(this.storage)).repositories()
                .toCompletableFuture().join(),
            Matchers.containsInAnyOrder("one", "two", "three")
        );
    }

    @Test
    void returnsPermissionsList() {
        final String john = "john";
        final String download = "download";
        final String upload = "upload";
        final String repo = "maven";
        final RepoPerms perm = new RepoPerms(
            new RepoPermissions.UserPermission(john, new ListOf<String>(download, upload))
        );
        perm.saveSettings(this.storage, repo);
        MatcherAssert.assertThat(
            new RepoPermissions.FromSettings(new Settings.Fake(this.storage)).permissions(repo)
                .toCompletableFuture().join(),
            Matchers.contains(
                new RepoPermissions.UserPermission(john, new ListOf<String>(download, upload))
            )
        );
    }

    @Test
    void returnsEmptyMapWhenPermissionsAreNotSet() {
        final String repo = "pypi";
        final RepoPerms perm = new RepoPerms();
        perm.saveSettings(this.storage, repo);
        MatcherAssert.assertThat(
            new RepoPermissions.FromSettings(new Settings.Fake(this.storage)).permissions(repo)
                .toCompletableFuture().join().size(),
            new IsEqual<>(0)
        );
    }

    @Test
    void updatesUserPermissions() throws IOException {
        final String repo = "rpm";
        final String david = "david";
        final String add = "add";
        final RepoPerms perm = new RepoPerms(
            new RepoPermissions.UserPermission(david, new ListOf<String>(add, "update"))
        );
        perm.saveSettings(this.storage, repo);
        final String olga = "olga";
        final String victor = "victor";
        final String download = "download";
        final String deploy = "deploy";
        new RepoPermissions.FromSettings(new Settings.Fake(this.storage))
            .addUpdate(
                repo,
                new ListOf<>(
                    new RepoPermissions.UserPermission(olga, new ListOf<>(download, deploy)),
                    new RepoPermissions.UserPermission(victor, new ListOf<>(deploy)),
                    new RepoPermissions.UserPermission(david, new ListOf<>(download, add))
                )
            ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Added permissions for olga",
            this.permissionsForUser(repo, olga),
            Matchers.contains(download, deploy)
        );
        MatcherAssert.assertThat(
            "Added permissions for victor",
            this.permissionsForUser(repo, victor),
            Matchers.contains(deploy)
        );
        MatcherAssert.assertThat(
            "Updated permissions for david",
            this.permissionsForUser(repo, david),
            Matchers.contains(download, add)
        );
    }

    @Test
    void addsUserPermissionWhenOriginalPermissionsAreNotSet() throws IOException {
        final String repo = "go";
        final RepoPerms perm = new RepoPerms();
        perm.saveSettings(this.storage, repo);
        final String ann = "ann";
        final String download = "download";
        new RepoPermissions.FromSettings(new Settings.Fake(this.storage))
            .addUpdate(
                repo,
                new ListOf<>(new RepoPermissions.UserPermission(ann, new ListOf<>(download)))
            )
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            this.permissionsForUser(repo, ann),
            Matchers.contains(download)
        );
    }

    @Test
    void deletesPermissionSection() throws IOException {
        final String repo = "nuget";
        final RepoPerms perm = new RepoPerms(
            new RepoPermissions.UserPermission("someone", new ListOf<String>("r", "w"))
        );
        perm.saveSettings(this.storage, repo);
        new RepoPermissions.FromSettings(new Settings.Fake(this.storage)).remove(repo)
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Permissions section are empty",
            this.permissionsSection(repo),
            new IsNull<>()
        );
        MatcherAssert.assertThat(
            "Storage `type` is intact",
            this.repoSection(repo).string("type"),
            new IsEqual<>("any")
        );
    }

    private List<String> permissionsForUser(final String repo, final String user)
        throws IOException {
        return this.permissionsSection(repo).yamlSequence(user)
            .values().stream().map(node -> node.asScalar().value())
            .collect(Collectors.toList());
    }

    private YamlMapping permissionsSection(final String repo) throws IOException {
        return this.repoSection(repo).yamlMapping("permissions");
    }

    private YamlMapping repoSection(final String repo) throws IOException {
        return Yaml.createYamlInput(
            new PublisherAs(this.storage.value(new Key.From(String.format("%s.yaml", repo))).join())
                .asciiString().toCompletableFuture().join()
        ).readYamlMapping().yamlMapping("repo");
    }
}
