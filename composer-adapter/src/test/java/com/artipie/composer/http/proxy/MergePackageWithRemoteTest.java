/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer.http.proxy;

import com.artipie.asto.Content;
import com.artipie.asto.test.TestResource;
import org.cactoos.set.SetOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.json.JsonObject;
import java.util.Optional;

/**
 * Test for {@link MergePackage.WithRemote}.
 * @since 0.4
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class MergePackageWithRemoteTest {
    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"packages\":{}}"})
    void returnsEmptyWhenLocalAndRemoteNotContainPackage(final String content) {
        final byte[] pkgs = content.getBytes();
        MatcherAssert.assertThat(
            this.mergedContent("not/exist", pkgs, pkgs).isPresent(),
            new IsEqual<>(false)
        );
    }

    @Test
    void returnsFromRemoteForEmptyLocal() {
        final String name = "psr/log";
        final byte[] remote = new TestResource("merge/remote.json").asBytes();
        final JsonObject pkgs = this.packagesFromMerged("{}".getBytes(), remote);
        MatcherAssert.assertThat(
            "Contains required package name",
            pkgs.keySet(),
            new IsEqual<>(new SetOf<>(name))
        );
        MatcherAssert.assertThat(
            "Contains all versions",
            pkgs.getJsonObject(name).keySet().toArray(),
            Matchers.arrayContainingInAnyOrder("1.1.2", "1.1.3")
        );
    }

    @Test
    void emptyWhenRemoteDoesNotContainsNameAndLocalIsEmpty() {
        final byte[] remote = new TestResource("merge/remote.json").asBytes();
        MatcherAssert.assertThat(
            this.mergedContent("not/exist", "{}".getBytes(), remote).isPresent(),
            new IsEqual<>(false)
        );
    }

    @Test
    void mergesLocalWithRemote() {
        final String name = "psr/log";
        final byte[] remote = new TestResource("merge/remote.json").asBytes();
        final byte[] local = new TestResource("merge/local.json").asBytes();
        final JsonObject pkgs = this.packagesFromMerged(local, remote);
        MatcherAssert.assertThat(
            "Contains required package name",
            pkgs.keySet(),
            new IsEqual<>(new SetOf<>(name))
        );
        MatcherAssert.assertThat(
            "Contains all versions",
            pkgs.getJsonObject(name).keySet(),
            Matchers.containsInAnyOrder("1.1.3", "1.1.4", "1.1.2")
        );
        for (final String vrsn: pkgs.getJsonObject(name).keySet()) {
            MatcherAssert.assertThat(
                "Each entry contains required fields",
                pkgs.getJsonObject(name)
                    .getJsonObject(vrsn)
                    .keySet(),
                Matchers.hasItems("version", "name", "dist", "uid")
            );
        }
    }

    @Test
    void returnsFromLocalForEmptyRemote() {
        final String name = "psr/log";
        final byte[] local = new TestResource("merge/local.json").asBytes();
        MatcherAssert.assertThat(
            this.packagesFromMerged(local, "{}".getBytes()).keySet(),
            new IsEqual<>(new SetOf<>(name))
        );
    }

    @Test
    void returnsEmptyWhenLocalAndRemoteContainAnotherPackage() {
        final byte[] local = new TestResource("merge/local.json").asBytes();
        final byte[] remote = new TestResource("merge/remote.json").asBytes();
        MatcherAssert.assertThat(
            this.mergedContent("not/exist", local, remote).isPresent(),
            new IsEqual<>(false)
        );
    }

    private Optional<Content> mergedContent(
        final String name, final byte[] local, final byte[] remote
    ) {
        return new MergePackage.WithRemote(name, new Content.From(local))
            .merge(
                Optional.of(new Content.From(remote))
            ).toCompletableFuture().join();
    }

    private JsonObject packagesFromMerged(final byte[] local, final byte[] remote) {
        return this.mergedContent("psr/log", local, remote)
            .orElseThrow().asJsonObject()
            .getJsonObject("packages");
    }
}
