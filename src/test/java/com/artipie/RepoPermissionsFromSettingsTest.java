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

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import java.io.IOException;
import java.util.List;
import org.cactoos.list.ListOf;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;
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
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
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
        final BuildingRepoPermissions perm = new BuildingRepoPermissions(this.storage);
        perm.addSettings(
            repo,
            new MapOf<String, List<String>>(
                new MapEntry<>(john, new ListOf<String>(download, upload))
            )
        );
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
        final BuildingRepoPermissions perm = new BuildingRepoPermissions(this.storage);
        perm.addEmpty(repo);
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
        final BuildingRepoPermissions perm = new BuildingRepoPermissions(this.storage);
        perm.addSettings(
            repo,
            new MapOf<String, List<String>>(
                new MapEntry<>(david, new ListOf<String>(add, "update"))
            )
        );
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
            perm.permissionsForUser(repo, olga),
            Matchers.contains(download, deploy)
        );
        MatcherAssert.assertThat(
            "Added permissions for victor",
            perm.permissionsForUser(repo, victor),
            Matchers.contains(deploy)
        );
        MatcherAssert.assertThat(
            "Updated permissions for david",
            perm.permissionsForUser(repo, david),
            Matchers.contains(download, add)
        );
    }

    @Test
    void addsUserPermissionWhenOriginalPermissionsAreNotSet() throws IOException {
        final String repo = "go";
        final BuildingRepoPermissions perm = new BuildingRepoPermissions(this.storage);
        perm.addEmpty(repo);
        final String ann = "ann";
        final String download = "download";
        new RepoPermissions.FromSettings(new Settings.Fake(this.storage))
            .addUpdate(
                repo,
                new ListOf<>(new RepoPermissions.UserPermission(ann, new ListOf<>(download)))
            )
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            perm.permissionsForUser(repo, ann),
            Matchers.contains(download)
        );
    }

    @Test
    void deletesPermissionSection() throws IOException {
        final String repo = "nuget";
        final BuildingRepoPermissions perm = new BuildingRepoPermissions(this.storage);
        perm.addSettings(
            repo,
            new MapOf<String, List<String>>(
                new MapEntry<>("someone", new ListOf<String>("r", "w"))
            )
        );
        new RepoPermissions.FromSettings(new Settings.Fake(this.storage)).remove(repo)
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Permissions section are empty",
            perm.permissionsSection(repo),
            new IsNull<>()
        );
        MatcherAssert.assertThat(
            "Storage `type` is intact",
            perm.repoSection(repo).string("type"),
            new IsEqual<>("any")
        );
    }
}
