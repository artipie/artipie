/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.helm.metadata;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.cactoos.set.SetOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test for {@link Index.WithBreaks}.
 * @since 0.3
 */
final class IndexWithBreaksTest {
    @ParameterizedTest
    @CsvSource({
        "index.yaml,''",
        "index/index-four-spaces.yaml,''",
        "index.yaml,prefix"
    })
    void returnsVersionsForPackages(final String index, final String prefix) throws IOException {
        final String tomcat = "tomcat";
        final String ark = "ark";
        final Key keyidx = new Key.From(new Key.From(prefix), IndexYaml.INDEX_YAML);
        final Storage storage = new InMemoryStorage();
        new BlockingStorage(storage).save(keyidx, new TestResource(index).asBytes());
        final Map<String, Set<String>> vrsns = new Index.WithBreaks(storage)
            .versionsByPackages(keyidx)
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Does not contain required packages",
            vrsns.keySet(),
            Matchers.containsInAnyOrder(ark, tomcat)
        );
        MatcherAssert.assertThat(
            "Parsed versions for `tomcat` are incorrect",
            vrsns.get(tomcat),
            new IsEqual<>(new SetOf<>("0.4.1"))
        );
        MatcherAssert.assertThat(
            "Parsed versions for `ark` are incorrect",
            vrsns.get(ark),
            Matchers.containsInAnyOrder("1.0.1", "1.2.0")
        );
    }
}
