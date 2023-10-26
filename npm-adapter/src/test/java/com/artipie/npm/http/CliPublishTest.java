/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.npm.http;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.npm.Publish;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link CliPublish}.
 * @since 0.9
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class CliPublishTest {

    @Test
    void metaFileAndTgzArchiveExist() {
        final Storage asto = new InMemoryStorage();
        final Key prefix = new Key.From("@hello/simple-npm-project");
        final Key name = new Key.From("uploaded-artifact");
        new TestResource("json/cli_publish.json").saveTo(asto, name);
        new CliPublish(asto).publish(prefix, name).join();
        MatcherAssert.assertThat(
            "Tgz archive was created",
            asto.exists(new Key.From(String.format("%s/-/%s-1.0.1.tgz", prefix, prefix))).join(),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Meta json file was create",
            asto.exists(new Key.From(prefix, "meta.json")).join(),
            new IsEqual<>(true)
        );
    }

    @Test
    void returnsCorrectPackageInfo() {
        final Storage asto = new InMemoryStorage();
        final Key prefix = new Key.From("@hello/simple-npm-project");
        final Key name = new Key.From("uploaded-artifact");
        new TestResource("json/cli_publish.json").saveTo(asto, name);
        final Publish.PackageInfo res = new CliPublish(asto).publishWithInfo(prefix, name).join();
        MatcherAssert.assertThat(
            "Tgz archive was created",
            asto.exists(new Key.From(String.format("%s/-/%s-1.0.1.tgz", prefix, prefix))).join(),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Meta json file was create",
            asto.exists(new Key.From(prefix, "meta.json")).join(),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Returns correct package name",
            res.packageName(), new IsEqual<>("@hello/simple-npm-project")
        );
        MatcherAssert.assertThat(
            "Returns correct package version",
            res.packageVersion(), new IsEqual<>("1.0.1")
        );
        MatcherAssert.assertThat(
            "Returns correct package version",
            res.tarSize(), new IsEqual<>(306L)
        );
    }

}
