/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.helm;

import com.artipie.asto.Content;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.helm.metadata.IndexYaml;
import com.artipie.helm.metadata.IndexYamlMapping;
import com.artipie.helm.test.ContentOfIndex;
import com.google.common.base.Throwables;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.digest.DigestUtils;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test case for {@link IndexYaml}.
 * @since 0.2
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
final class IndexYamlTest {

    /**
     * Chart name.
     */
    private static final String TOMCAT = "tomcat-0.4.1.tgz";

    /**
     * Chart name.
     */
    private static final String ARK = "ark-1.0.1.tgz";

    /**
     * Storage.
     */
    private Storage storage;

    /**
     * Index yaml file.
     */
    private IndexYaml yaml;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
        this.yaml = new IndexYaml(this.storage);
    }

    @Test
    void verifyDigestFromIndex() {
        this.update(IndexYamlTest.TOMCAT);
        final List<Map<String, Object>> tomcat = new ContentOfIndex(this.storage).index()
            .byChart("tomcat");
        MatcherAssert.assertThat(
            tomcat.get(0).get("digest"),
            new IsEqual<>(
                DigestUtils.sha256Hex(new TestResource(IndexYamlTest.TOMCAT).asBytes())
            )
        );
    }

    @Test
    void notChangeForSameChartWithSameVersion() {
        this.update(IndexYamlTest.TOMCAT);
        final String tomcat = "tomcat";
        final Map<String, Object> old = new ContentOfIndex(this.storage).index()
            .byChart(tomcat).get(0);
        this.update(IndexYamlTest.TOMCAT);
        final List<Map<String, Object>> updt = new ContentOfIndex(this.storage).index()
            .byChart(tomcat);
        MatcherAssert.assertThat(
            "New version was not added",
            updt.size(),
            new IsEqual<>(1)
        );
        MatcherAssert.assertThat(
            "Metadata was not changed",
            old.equals(updt.get(0)),
            new IsEqual<>(true)
        );
    }

    @Test
    void addMetadataForSameChartWithNewVersion() {
        this.update(IndexYamlTest.TOMCAT);
        this.update(IndexYamlTest.ARK);
        this.update("ark-1.2.0.tgz");
        final List<Map<String, Object>> entries = new ContentOfIndex(this.storage)
            .index().byChart("ark");
        MatcherAssert.assertThat(
            "New version was added",
            entries.size(),
            new IsEqual<>(2)
        );
        final String[] versions = entries.stream()
            .map(entry -> (String) entry.get("version"))
            .toArray(String[]::new);
        MatcherAssert.assertThat(
            "Contains both versions",
            versions,
            Matchers.arrayContainingInAnyOrder("1.0.1", "1.2.0")
        );
    }

    @Test
    void addMetadataForNewChartInExistingIndex() {
        this.update(IndexYamlTest.TOMCAT);
        this.update(IndexYamlTest.ARK);
        final Map<String, Object> ark = new ContentOfIndex(this.storage).index()
            .byChart("ark").get(0);
        final Map<String, Object> chart = this.chartYaml(IndexYamlTest.ARK);
        final int numgenfields = 3;
        MatcherAssert.assertThat(
            "Index.yaml has required number of keys",
            ark.size(),
            new IsEqual<>(chart.size() + numgenfields)
        );
        MatcherAssert.assertThat(
            "Keys have correct values",
            ark,
            new AllOf<>(
                Arrays.asList(
                    this.matcher("appVersion", chart),
                    this.matcher("apiVersion", chart),
                    this.matcher("version", chart),
                    this.matcher("name", chart),
                    this.matcher("description", chart),
                    this.matcher("home", chart),
                    this.matcher("maintainers", chart),
                    Matchers.hasEntry("urls", Collections.singletonList(IndexYamlTest.ARK)),
                    Matchers.hasEntry(
                        "sources", Collections.singletonList("https://github.com/heptio/ark")
                    ),
                    Matchers.hasKey("created"),
                    Matchers.hasKey("digest")
                )
            )
        );
    }

    @Test
    void deleteChartByNameFromIndexYaml() {
        new TestResource("index.yaml").saveTo(this.storage);
        this.yaml.deleteByName("ark").blockingGet();
        final IndexYamlMapping mapping = new ContentOfIndex(this.storage).index();
        MatcherAssert.assertThat(
            "Number of charts is correct",
            mapping.entries().size(),
            new IsEqual<>(1)
        );
        MatcherAssert.assertThat(
            "Correct chart was deleted",
            mapping.entries().containsKey("tomcat"),
            new IsEqual<>(true)
        );
    }

    @Test
    void failsToDeleteChartByNameWhenIndexYamlAbsent() {
        MatcherAssert.assertThat(
            Throwables.getRootCause(this.yaml.deleteByName("ark").blockingGet()),
            new IsInstanceOf(FileNotFoundException.class)
        );
    }

    @Test
    void deleteChartByNameVersionWithManyVersionsFromIndex() {
        final String chart = "ark";
        new TestResource("index.yaml").saveTo(this.storage);
        this.yaml.deleteByNameAndVersion(chart, "1.0.1").blockingGet();
        final IndexYamlMapping mapping = new ContentOfIndex(this.storage).index();
        MatcherAssert.assertThat(
            "Number of versions of chart is correct",
            mapping.byChart(chart).size(),
            new IsEqual<>(1)
        );
        MatcherAssert.assertThat(
            "Correct version of chart was deleted",
            mapping.byChartAndVersion(chart, "1.2.0").isPresent(),
            new IsEqual<>(true)
        );
    }

    @Test
    void deleteChartByNameVersionWithSingleVersionFromIndex() {
        final String chart = "tomcat";
        new TestResource("index.yaml").saveTo(this.storage);
        this.yaml.deleteByNameAndVersion(chart, "0.4.1").blockingGet();
        MatcherAssert.assertThat(
            new ContentOfIndex(this.storage).index()
                .entries().containsKey(chart),
            new IsEqual<>(false)
        );
    }

    @Test
    void deleteAbsentChartByNameFromIndex() {
        new TestResource("index.yaml").saveTo(this.storage);
        this.yaml.deleteByName("absent").blockingGet();
        MatcherAssert.assertThat(
            new ContentOfIndex(this.storage).index()
                .entries().size(),
            new IsEqual<>(2)
        );
    }

    @Test
    void deleteChartByNameAndAbsentVersionFromIndex() {
        final String chart = "tomcat";
        new TestResource("index.yaml").saveTo(this.storage);
        this.yaml.deleteByNameAndVersion(chart, "0.0.0").blockingGet();
        MatcherAssert.assertThat(
            new ContentOfIndex(this.storage).index()
                .byChart(chart).size(),
            new IsEqual<>(1)
        );
    }

    private Matcher<Map<? extends String, ?>> matcher(final String key,
        final Map<String, Object> chart) {
        return Matchers.hasEntry(key, chart.get(key));
    }

    private Map<String, Object> chartYaml(final String file) {
        return new TgzArchive(
            new PublisherAs(
                new Content.From(new TestResource(file).asBytes())
            ).bytes()
            .toCompletableFuture().join()
        ).chartYaml()
        .fields();
    }

    private void update(final String chart) {
        this.yaml.update(
            new TgzArchive(
                new PublisherAs(
                    new Content.From(new TestResource(chart).asBytes())
                ).bytes()
                .toCompletableFuture().join()
            )
        ).blockingGet();
    }
}
