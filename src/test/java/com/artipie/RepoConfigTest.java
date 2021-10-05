/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Key;
import com.artipie.asto.test.TestResource;
import com.artipie.http.client.ClientSlices;

import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ExecutionException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link RepoConfig}.
 * @since 0.2
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.AvoidDuplicateLiterals"})
public final class RepoConfigTest {

    /**
     * HTTP client instance.
     */
    private final ClientSlices http;

    /**
     * Ctor.
     */
    public RepoConfigTest() {
        this.http = new JettyClientSlicesAutoStarted();
    }

    @Test
    public void readsCustom() throws Exception {
        final RepoConfig config = this.readFull();
        final YamlMapping yaml = config.settings().orElseThrow();
        MatcherAssert.assertThat(
            yaml.string("custom-property"),
            new IsEqual<>("custom-value")
        );
    }

    @Test
    public void failsToReadCustom() throws Exception {
        final RepoConfig config = this.readMin();
        MatcherAssert.assertThat(
            "Unexpected custom config",
            config.settings().isEmpty()
        );
    }

    @Test
    public void readContentLengthMax() throws Exception {
        final RepoConfig config = this.readFull();
        final long value = 123L;
        MatcherAssert.assertThat(
            config.contentLengthMax(),
            new IsEqual<>(Optional.of(value))
        );
    }

    @Test
    public void readEmptyContentLengthMax() throws Exception {
        final RepoConfig config = this.readMin();
        MatcherAssert.assertThat(
            config.contentLengthMax().isEmpty(),
            new IsEqual<>(true)
        );
    }

    @Test
    public void readsPortWhenSpecified() throws Exception {
        final RepoConfig config = this.readFull();
        final int expected = 1234;
        MatcherAssert.assertThat(
            config.port(),
            new IsEqual<>(OptionalInt.of(expected))
        );
    }

    @Test
    public void readsEmptyPortWhenNotSpecified() throws Exception {
        final RepoConfig config = this.readMin();
        MatcherAssert.assertThat(
            config.port(),
            new IsEqual<>(OptionalInt.empty())
        );
    }

    @Test
    public void readsRepositoryTypeRepoPart() throws Exception {
        final RepoConfig config = this.readMin();
        MatcherAssert.assertThat(
            config.type(),
            new IsEqual<>("maven")
        );
    }

    @Test
    public void throwExceptionWhenPathNotSpecified() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> this.repoCustom().path()
        );
    }

    @Test
    public void getEmptyWhenPermissionsNotSpecified() throws Exception {
        MatcherAssert.assertThat(
            this.readMin().permissions(),
            new IsEqual<>(Optional.empty())
        );
    }

    @Test
    public void getPermissionsPart() throws Exception {
        MatcherAssert.assertThat(
            this.readFull().permissions().get(),
            new IsInstanceOf(YamlPermissions.class)
        );
    }

    @Test
    public void getPathPart() throws Exception {
        MatcherAssert.assertThat(
            this.readFull().path(),
            new IsEqual<>("mvn")
        );
    }

    @Test
    public void getUrlWhenUrlIsCorrect() {
        final String target = "http://host:8080/correct";
        MatcherAssert.assertThat(
            this.repoCustom("url", target).url().toString(),
            new IsEqual<>(target)
        );
    }

    @Test
    public void throwExceptionWhenUrlIsMalformed() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> this.repoCustom("url", "host:8080/without/scheme").url()
        );
    }

    @Test
    public void throwsExceptionWhenStorageWithDefaultAliasesNotConfigured() {
        MatcherAssert.assertThat(
            Assertions.assertThrows(
                IllegalStateException.class,
                () -> this.repoCustom().storage()
            ).getMessage(),
            new IsEqual<>("Storage is not configured")
        );
    }

    @Test
    public void throwsExceptionForInvalidStorageConfig() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new RepoConfig(
                this.http,
                StorageAliases.EMPTY,
                new Key.From("key"),
                Yaml.createYamlMappingBuilder().add(
                    "repo", Yaml.createYamlMappingBuilder()
                        .add(
                            "storage", Yaml.createYamlSequenceBuilder()
                                .add("wrong because sequence").build()
                        ).build()
                ).build()
            ).storage()
        );
    }

    private RepoConfig readFull() throws Exception {
        return this.readFromResource("repo-full-config.yml");
    }

    private RepoConfig readMin() throws Exception {
        return this.readFromResource("repo-min-config.yml");
    }

    private RepoConfig repoCustom() {
        return this.repoCustom("url", "http://host:8080/correct");
    }

    private RepoConfig repoCustom(final String name, final String value) {
        return new RepoConfig(
            this.http,
            StorageAliases.EMPTY,
            new Key.From("repo-custom.yml"),
            Yaml.createYamlMappingBuilder().add(
                "repo", Yaml.createYamlMappingBuilder()
                    .add("type", "maven")
                    .add(name, value)
                    .build()
            ).build()
        );
    }

    private RepoConfig readFromResource(final String name)
        throws ExecutionException, InterruptedException {
        return RepoConfig.fromPublisher(
            this.http,
            StorageAliases.EMPTY,
            new Key.From(name),
            Flowable.just(
                ByteBuffer.wrap(new TestResource(name).asBytes())
            )
        ).toCompletableFuture().get();
    }
}
