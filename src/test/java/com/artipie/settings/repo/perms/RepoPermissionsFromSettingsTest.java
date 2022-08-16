/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.repo.perms;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.settings.repo.ConfigFile;
import com.artipie.settings.repo.RepoConfigYaml;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsEmptyCollection;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link RepoPermissionsFromStorage}.
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
        this.storage.save(new Key.From("four.yml"), Content.EMPTY).join();
        MatcherAssert.assertThat(
            new RepoPermissionsFromStorage(this.storage).repositories()
                .toCompletableFuture().join(),
            Matchers.containsInAnyOrder("one", "two", "three", "four")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {".yaml", ".yml", ""})
    void returnsPermissionsListForDifferentExtensions(final String extension) {
        final String john = "john";
        final String download = "download";
        final String upload = "upload";
        final String repo = "maven";
        new RepoConfigYaml(repo).withPermissions(
            new RepoPerms(john, new ListOf<String>(download, upload))
        ).saveTo(this.storage, repo);
        MatcherAssert.assertThat(
            new RepoPermissionsFromStorage(this.storage)
                .permissions(String.format("%s%s", repo, extension))
                .toCompletableFuture().join(),
            Matchers.contains(
                new RepoPermissions.PermissionItem(john, new ListOf<String>(download, upload))
            )
        );
    }

    @Test
    void returnsEmptyMapWhenPermissionsAreNotSet() {
        final String repo = "pypi";
        new RepoConfigYaml(repo).saveTo(this.storage, repo);
        MatcherAssert.assertThat(
            new RepoPermissionsFromStorage(this.storage).permissions(repo)
                .toCompletableFuture().join().size(),
            new IsEqual<>(0)
        );
    }

    @Test
    void returnsPatternsList() {
        final String repo = "docker";
        new RepoConfigYaml(repo).withPermissions(
            new RepoPerms(Collections.emptyList(), new ListOf<>("**"))
        ).saveTo(this.storage, repo);
        MatcherAssert.assertThat(
            new RepoPermissionsFromStorage(this.storage).patterns(repo)
                .toCompletableFuture().join()
                .stream().map(RepoPermissions.PathPattern::string).collect(Collectors.toList()),
            Matchers.contains("**")
        );
    }

    @Test
    void returnsPatternsListWhenEmpty() {
        final String repo = "gem";
        new RepoConfigYaml(repo).saveTo(this.storage, repo);
        MatcherAssert.assertThat(
            new RepoPermissionsFromStorage(this.storage).patterns(repo)
                .toCompletableFuture().join()
                .stream().map(RepoPermissions.PathPattern::string).collect(Collectors.toList()),
            new IsEmptyCollection<>()
        );
    }

    @Test
    void updatesUserPermissionsAndPatterns() throws IOException {
        final String repo = "rpm";
        final String david = "david";
        final String add = "add";
        new RepoConfigYaml(repo).withPermissions(
            new RepoPerms(
                Collections.singleton(
                    new RepoPermissions.PermissionItem(david, new ListOf<String>(add, "update"))
                ),
                new ListOf<>("**")
            )
        ).saveTo(this.storage, repo);
        final String olga = "olga";
        final String victor = "victor";
        final String download = "download";
        final String deploy = "deploy";
        new RepoPermissionsFromStorage(this.storage)
            .update(
                repo,
                new ListOf<>(
                    new RepoPermissions.PermissionItem(olga, new ListOf<>(download, deploy)),
                    new RepoPermissions.PermissionItem(victor, new ListOf<>(deploy)),
                    new RepoPermissions.PermissionItem(david, new ListOf<>(download, add))
                ),
                new ListOf<>(new RepoPermissions.PathPattern("rpm/*"))
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
        MatcherAssert.assertThat(
            "Updated patterns",
            this.patterns(repo),
            Matchers.contains("rpm/*")
        );
    }

    @Test
    void addsUserPermissionsAndPatternsWhenEmpty() throws IOException {
        final String repo = "go";
        new RepoConfigYaml(repo).saveTo(this.storage, repo);
        final String ann = "ann";
        final String download = "download";
        new RepoPermissionsFromStorage(this.storage)
            .update(
                repo,
                new ListOf<>(new RepoPermissions.PermissionItem(ann, new ListOf<>(download))),
                new ListOf<>(new RepoPermissions.PathPattern("**"))
            )
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Updated user permissions for ann",
            this.permissionsForUser(repo, ann),
            Matchers.contains(download)
        );
        MatcherAssert.assertThat(
            "Updated patterns",
            this.patterns(repo),
            Matchers.contains("**")
        );
    }

    @Test
    void deletesPermissionSection() throws IOException {
        final String repo = "nuget";
        new RepoConfigYaml(repo).withPermissions(
            new RepoPerms("someone", new ListOf<>("r", "w"))
        ).saveTo(this.storage, repo);
        new RepoPermissionsFromStorage(this.storage).remove(repo)
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Permissions section are empty",
            this.permissionsSection(repo),
            new IsNull<>()
        );
        MatcherAssert.assertThat(
            "Storage `type` is intact",
            this.repoSection(repo).string("type"),
            new IsEqual<>(repo)
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
            new PublisherAs(
                new ConfigFile(repo).valueFrom(this.storage)
                    .toCompletableFuture().join()
            ).asciiString()
            .toCompletableFuture().join()
        ).readYamlMapping().yamlMapping("repo");
    }

    private List<String> patterns(final String repo)
        throws IOException {
        return this.repoSection(repo)
            .yamlSequence("permissions_include_patterns")
            .values().stream().map(node -> node.asScalar().value())
            .collect(Collectors.toList());
    }
}
