/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.helm;

import com.artipie.ArtipieException;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.helm.metadata.IndexYaml;
import com.artipie.helm.metadata.IndexYamlMapping;
import com.artipie.helm.test.ContentOfIndex;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link RemoveWriter.Asto}.
 * @since 0.3
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class RemoveWriterAstoTest {
    /**
     * Temporary directory for all tests.
     */
    @TempDir
    Path dir;

    /**
     * Key to source index file.
     */
    private Key source;

    /**
     * Path for index file where it will rewritten.
     */
    private Path out;

    /**
     * Storage.
     */
    private Storage storage;

    @BeforeEach
    void setUp() throws IOException {
        final String prfx = "index-";
        this.source = IndexYaml.INDEX_YAML;
        this.out = Files.createTempFile(this.dir, prfx, "-out.yaml");
        this.storage = new FileStorage(this.dir);
    }

    @ParameterizedTest
    @ValueSource(strings = {"index.yaml", "index/index-four-spaces.yaml"})
    void deletesOneOfManyVersionOfChart(final String idx) {
        final String chart = "ark-1.0.1.tgz";
        new TestResource(idx).saveTo(this.storage, this.source);
        new TestResource(chart).saveTo(this.storage);
        this.delete(chart);
        final IndexYamlMapping index = new ContentOfIndex(this.storage).index(this.pathToIndex());
        MatcherAssert.assertThat(
            "Removed version exists",
            index.byChartAndVersion("ark", "1.0.1").isPresent(),
            new IsEqual<>(false)
        );
        MatcherAssert.assertThat(
            "Extra version of chart was deleted",
            index.byChartAndVersion("ark", "1.2.0").isPresent(),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Extra chart was deleted",
            index.byChartAndVersion("tomcat", "0.4.1").isPresent(),
            new IsEqual<>(true)
        );
    }

    @Test
    void deletesAllVersionOfChart() {
        final String arkone = "ark-1.0.1.tgz";
        final String arktwo = "ark-1.2.0.tgz";
        new TestResource("index.yaml").saveTo(this.storage, this.source);
        new TestResource(arkone).saveTo(this.storage);
        new TestResource(arktwo).saveTo(this.storage);
        this.delete(arkone, arktwo);
        final IndexYamlMapping index = new ContentOfIndex(this.storage).index(this.pathToIndex());
        MatcherAssert.assertThat(
            "Removed versions exist",
            index.byChart("ark").isEmpty(),
            new IsEqual<>(true)
        );
        MatcherAssert.assertThat(
            "Extra chart was deleted",
            index.byChartAndVersion("tomcat", "0.4.1").isPresent(),
            new IsEqual<>(true)
        );
    }

    @Test
    void deleteLastChartFromIndex() {
        final String chart = "ark-1.0.1.tgz";
        new TestResource("index/index-one-ark.yaml").saveTo(this.storage, this.source);
        new TestResource(chart).saveTo(this.storage);
        this.delete(chart);
        MatcherAssert.assertThat(
            new ContentOfIndex(this.storage).index(this.pathToIndex())
                .entries().isEmpty(),
            new IsEqual<>(true)
        );
    }

    @Test
    void failsToDeleteAbsentInIndexChart() {
        final String chart = "tomcat-0.4.1.tgz";
        new TestResource("index/index-one-ark.yaml").saveTo(this.storage, this.source);
        new TestResource(chart).saveTo(this.storage);
        final Throwable thr = Assertions.assertThrows(
            CompletionException.class,
            () -> this.delete(chart)
        );
        MatcherAssert.assertThat(
            thr.getCause(),
            new IsInstanceOf(ArtipieException.class)
        );
    }

    private void delete(final String... charts) {
        final Collection<Key> keys = Arrays.stream(charts)
            .map(Key.From::new)
            .collect(Collectors.toList());
        final Map<String, Set<String>> todelete = new HashMap<>();
        keys.forEach(
            key -> {
                final ChartYaml chart = new TgzArchive(
                    new PublisherAs(this.storage.value(key).join()).bytes()
                        .toCompletableFuture().join()
                ).chartYaml();
                todelete.putIfAbsent(chart.name(), new HashSet<>());
                todelete.get(chart.name()).add(chart.version());
            }
        );
        new RemoveWriter.Asto(this.storage)
            .delete(this.source, this.out, todelete)
            .toCompletableFuture().join();
    }

    private Key pathToIndex() {
        return new Key.From(this.out.getFileName().toString());
    }
}
