/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven.http;

import com.artipie.asto.Key;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test case for {@link ArtifactHeaders}.
 *
 * @since 1.0
 * @checkstyle JavadocMethodCheck (500 lines)
 */
public final class ArtifactHeadersTest {

    @Test
    void addsChecksumAndEtagHeaders() {
        final String one = "one";
        final String two = "two";
        final String three = "three";
        MatcherAssert.assertThat(
            StreamSupport.stream(
                new ArtifactHeaders(
                    new Key.From("anything"),
                    new MapOf<>(
                        new MapEntry<>("sha1", one),
                        new MapEntry<>("sha256", two),
                        new MapEntry<>("sha512", three)
                    )
                ).spliterator(), false
            ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
            Matchers.allOf(
                Matchers.hasEntry("X-Checksum-sha1", one),
                Matchers.hasEntry("X-Checksum-sha256", two),
                Matchers.hasEntry("X-Checksum-sha512", three),
                Matchers.hasEntry("ETag", one)
            )
        );
    }

    @Test
    void addsContentDispositionHeader() {
        MatcherAssert.assertThat(
            StreamSupport.stream(
                new ArtifactHeaders(
                    new Key.From("artifact.jar"),
                    Collections.emptyNavigableMap()
                ).spliterator(), false
            ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
            Matchers.hasEntry("Content-Disposition", "attachment; filename=\"artifact.jar\"")
        );
    }

    @CsvSource({
        "target.jar,application/java-archive",
        "target.pom,application/x-maven-pom+xml",
        "target.xml,application/xml",
        "target.none,*"
    })
    @ParameterizedTest
    void addsContentTypeHeaders(final String target, final String mime) {
        MatcherAssert.assertThat(
            StreamSupport.stream(
                new ArtifactHeaders(
                    new Key.From(target), Collections.emptyNavigableMap()
                ).spliterator(), false
            ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
            Matchers.hasEntry("Content-Type", mime)
        );
    }
}
