/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.helm;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.cactoos.list.ListOf;
import org.cactoos.set.SetOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link Charts.Asto}.
 * @since 0.3
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class ChartsAstoTest {
    /**
     * Storage.
     */
    private Storage storage;

    @BeforeEach
    void setUp() {
        this.storage = new InMemoryStorage();
    }

    @Test
    void getsVersionsForPassedCharts() {
        final String arkone = "ark-1.0.1.tgz";
        final String arktwo = "ark-1.2.0.tgz";
        final String tomcat = "tomcat-0.4.1.tgz";
        Stream.of(arkone, arktwo, tomcat)
            .forEach(tgz -> new TestResource(tgz).saveTo(this.storage));
        final Map<String, Set<String>> expected = new HashMap<>();
        expected.put("tomcat", new SetOf<String>("0.4.1"));
        expected.put("ark", new SetOf<String>("1.2.0", "1.0.1"));
        MatcherAssert.assertThat(
            new Charts.Asto(this.storage)
                .versionsFor(
                    Stream.of(arkone, arktwo, tomcat)
                        .map(Key.From::new)
                        .collect(Collectors.toList())
                ).toCompletableFuture().join(),
            new IsEqual<>(expected)
        );
    }

    @Test
    void getsVersionsAndYamlForPassedChart() {
        final String tomcat = "tomcat";
        final String tomcattgz = "tomcat-0.4.1.tgz";
        new TestResource(tomcattgz).saveTo(this.storage);
        final Map<String, Set<Pair<String, ChartYaml>>> got;
        got = new Charts.Asto(this.storage)
            .versionsAndYamlFor(new ListOf<Key>(new Key.From(tomcattgz)))
            .toCompletableFuture().join();
        final Pair<String, ChartYaml> entry = got.get(tomcat).iterator().next();
        MatcherAssert.assertThat(
            "Chart names is incorrect",
            got.keySet(),
            new IsEqual<>(new SetOf<String>(tomcat))
        );
        MatcherAssert.assertThat(
            "Versions of tomcat are incorrect",
            entry.getLeft(),
            new IsEqual<>("0.4.1")
        );
        final Map<String, Object> gotchart = entry.getRight().fields();
        gotchart.remove("created");
        gotchart.remove("digest");
        gotchart.remove("urls");
        MatcherAssert.assertThat(
            "Chart yaml file is wrong",
            gotchart,
            new IsEqual<>(
                new TgzArchive(
                new PublisherAs(
                    this.storage.value(new Key.From(tomcattgz)).join()
                ).bytes()
                .toCompletableFuture().join()
            ).chartYaml().fields()
            )
        );
    }
}
