/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.helm;

import com.artipie.asto.test.TestResource;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsMapContaining;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Test;

/**
 * A test for {@link TgzArchive}.
 *
 * @since 0.2
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class TgzArchiveTest {

    @Test
    public void nameIdentifiedCorrectly() throws IOException {
        MatcherAssert.assertThat(
            new TgzArchive(
                new TestResource("tomcat-0.4.1.tgz").asBytes()
            ).name(),
            new IsEqual<>("tomcat-0.4.1.tgz")
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void hasCorrectMetadata() {
        MatcherAssert.assertThat(
            new TgzArchive(
                new TestResource("tomcat-0.4.1.tgz").asBytes()
            ).metadata(Optional.empty()),
            new AllOf<>(
                new ListOf<>(
                    new IsMapContaining<>(
                        new IsEqual<>("urls"),
                        new IsEqual<>(Collections.singletonList("tomcat-0.4.1.tgz"))
                    ),
                    new IsMapContaining<>(
                        new IsEqual<>("digest"),
                        new IsInstanceOf(String.class)
                    )
                )
            )
        );
    }
}
