/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.composer.http;

import com.artipie.asto.Content;
import com.artipie.asto.test.TestResource;
import com.artipie.composer.test.EmptyZip;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletionException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Archive.Zip}.
 * @since 0.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class ArchiveZipTest {
    @Test
    void obtainingComposerJsonWorks() {
        MatcherAssert.assertThat(
            new Archive.Zip(
                new Archive.Name("name", "1.0.1")
            ).composerFrom(new Content.From(new TestResource("log-1.1.3.zip").asBytes()))
            .toCompletableFuture().join()
            .toString(),
            new IsEqual<>(
                String.join(
                    "",
                    "{",
                    "\"name\":\"psr/log\",",
                    "\"description\":\"Common interface for logging libraries\",",
                    "\"keywords\":[\"psr\",\"psr-3\",\"log\"],",
                    "\"homepage\":\"https://github.com/php-fig/log\",",
                    "\"license\":\"MIT\",",
                    "\"authors\":[{\"name\":\"PHP-FIG\",",
                    "\"homepage\":\"http://www.php-fig.org/\"}],",
                    "\"require\":{\"php\":\">=5.3.0\"},",
                    "\"autoload\":{\"psr-4\":{\"Psr\\\\Log\\\\\":\"Psr/Log/\"}},",
                    "\"extra\":{\"branch-alias\":{\"dev-master\":\"1.1.x-dev\"}}",
                    "}"
                )
            )
        );
    }

    @Test
    void replacesComposerWithAnotherOne() {
        final byte[] target = new TestResource("log-composer-with-version.json").asBytes();
        final String full = "log-1.1.3.zip";
        final Archive.Name name = new Archive.Name(full, "1.1.3");
        final Content updarch = new Archive.Zip(name)
            .replaceComposerWith(
                new Content.From(new TestResource(full).asBytes()),
                target
            ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            new Archive.Zip(name)
                .composerFrom(updarch)
                .toCompletableFuture().join()
                .toString()
                .getBytes(StandardCharsets.UTF_8),
            new IsEqual<>(target)
        );
    }

    @Test
    void failsToObtainWhenFileIsAbsent() {
        final Exception exc = Assertions.assertThrows(
            CompletionException.class,
            () -> new Archive.Zip(
                new Archive.Name("some name", "1.0.2")
            ).composerFrom(new Content.From(new EmptyZip().value()))
            .toCompletableFuture().join()
        );
        MatcherAssert.assertThat(
            exc.getCause().getMessage(),
            new IsEqual<>("'composer.json' file was not found")
        );
    }
}
