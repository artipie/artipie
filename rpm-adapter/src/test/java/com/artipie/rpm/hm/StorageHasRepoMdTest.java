/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm.hm;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.rpm.Digest;
import com.artipie.rpm.RepoConfig;
import com.artipie.rpm.StandardNamingPolicy;
import org.hamcrest.core.IsNot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.llorllale.cactoos.matchers.Assertion;

/**
 * Tests for {@link StorageHasRepoMd} matcher.
 *
 * @since 1.1
 */
public final class StorageHasRepoMdTest {

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void matchPositive(final boolean filelists) {
        final Storage storage = new InMemoryStorage();
        new TestResource(
            "repodata/StorageHasRepoMdTest/repomd.xml"
        ).saveTo(storage, new Key.From("repodata/repomd.xml"));
        new TestResource(
            "repodata/StorageHasRepoMdTest/primary.xml.gz"
        ).saveTo(storage, new Key.From("repodata/primary.xml.gz"));
        new TestResource(
            "repodata/StorageHasRepoMdTest/other.xml.gz"
        ).saveTo(storage, new Key.From("repodata/other.xml.gz"));
        if (filelists) {
            new TestResource(
                "repodata/StorageHasRepoMdTest/filelists.xml.gz"
            ).saveTo(storage, new Key.From("repodata/filelists.xml.gz"));
        }
        new Assertion<>(
            "The matcher gives positive result for a valid repomd.xml configuration",
            storage,
            new StorageHasRepoMd(
                new RepoConfig.Simple(Digest.SHA256, StandardNamingPolicy.PLAIN, filelists)
            )
        ).affirm();
    }

    @Test
    public void doNotMatchesWhenRepomdAbsent() {
        new Assertion<>(
            "The matcher gives a negative result when storage does not have repomd.xml",
            new InMemoryStorage(),
            new IsNot<>(new StorageHasRepoMd(new RepoConfig.Simple()))
        ).affirm();
    }
}
