/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm;

import java.nio.file.Path;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link CliArguments}.
 *
 * @since 0.9
 */
@SuppressWarnings("PMD.TooManyMethods")
class CliArgumentsTest {

    @Test
    void canParseRepositoryArgument(@TempDir final Path temp) {
        MatcherAssert.assertThat(
            new CliArguments(
                String.format(
                    "%s",
                    temp.getFileName()
                )
            ).repository(),
            new IsEqual<>(temp.getFileName())
        );
    }

    @Test
    void canParseNamingPolicyArgument() {
        MatcherAssert.assertThat(
            new CliArguments(
                "-nsha1"
            ).config().naming(),
            new IsEqual<>(StandardNamingPolicy.SHA1)
        );
    }

    @Test
    void canParseFileListsArgument() {
        MatcherAssert.assertThat(
            new CliArguments(
                "-ffalse"
            ).config().filelists(),
            new IsEqual<>(false)
        );
    }

    @Test
    void canParseDigestArgument() {
        MatcherAssert.assertThat(
            new CliArguments(
                "-dsha1"
            ).config().digest(),
            new IsEqual<>(Digest.SHA1)
        );
    }

    @Test
    void canParseNamingPolicyArgumentWithEquals() {
        MatcherAssert.assertThat(
            new CliArguments(
                "-n=plain"
            ).config().naming(),
            new IsEqual<>(StandardNamingPolicy.PLAIN)
        );
    }

    @Test
    void canParseFileListsArgumentWithEquals() {
        MatcherAssert.assertThat(
            new CliArguments(
                "-f=true"
            ).config().filelists(),
            new IsEqual<>(true)
        );
    }

    @Test
    void canParseDigestArgumentWithEquals() {
        MatcherAssert.assertThat(
            new CliArguments(
                "-d=sha256"
            ).config().digest(),
            new IsEqual<>(Digest.SHA256)
        );
    }

    @Test
    void canParseNamingPolicyArgumentWithLongopt() {
        MatcherAssert.assertThat(
            new CliArguments(
                "-naming-policy=sha256"
            ).config().naming(),
            new IsEqual<>(StandardNamingPolicy.SHA256)
        );
    }

    @Test
    void canParseFileListsArgumentWithLongopt() {
        MatcherAssert.assertThat(
            new CliArguments(
                "-filelists=false"
            ).config().filelists(),
            new IsEqual<>(false)
        );
    }

    @Test
    void canParseDigestArgumentWithLongopt() {
        MatcherAssert.assertThat(
            new CliArguments(
                "-digest=sha1"
            ).config().digest(),
            new IsEqual<>(Digest.SHA1)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"-u0 1 * * *", "-update=1 * * * *"})
    void canParseModeArgument(final String input) {
        MatcherAssert.assertThat(
            new CliArguments(input).config().mode(),
            new IsEqual<>(RepoConfig.UpdateMode.CRON)
        );
    }

    @Test
    void canParseCronArgument() {
        MatcherAssert.assertThat(
            new CliArguments("-u* * 0 * *").config().cron().get(),
            new IsEqual<>("* * 0 * *")
        );
    }

    @Test
    void canParseCronArgumentWithLongopt() {
        MatcherAssert.assertThat(
            new CliArguments("-update=4 * * * *").config().cron().get(),
            new IsEqual<>("4 * * * *")
        );
    }

    @Test
    void returnEmptyCronIfOptionNot() {
        MatcherAssert.assertThat(
            new CliArguments("").config().cron().isPresent(),
            new IsEqual<>(false)
        );
    }
}
