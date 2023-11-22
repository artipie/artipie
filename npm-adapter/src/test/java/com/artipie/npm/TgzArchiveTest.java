/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm;

import com.artipie.ArtipieException;
import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.test.TestResource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import javax.json.JsonObject;
import org.hamcrest.MatcherAssert;
import org.hamcrest.beans.HasPropertyWithValue;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TgzArchive}.
 * @since 0.9
 * @checkstyle LineLengthCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class TgzArchiveTest {
    @Test
    void getProjectNameAndVersionFromPackageJson() {
        final JsonObject json = new TgzArchive(
            new String(
                new TestResource("binaries/vue-cli-plugin-liveapp-1.2.5.tgz").asBytes(),
                StandardCharsets.ISO_8859_1
            ),
            false
        ).packageJson();
        MatcherAssert.assertThat(
            "Name is parsed properly from package.json",
            json.getJsonString("name").getString(),
            new IsEqual<>("@aurora/vue-cli-plugin-liveapp")
        );
        MatcherAssert.assertThat(
            "Version is parsed properly from package.json",
            json.getJsonString("version").getString(),
            new IsEqual<>("1.2.5")
        );
    }

    @Test
    void getArchiveEncoded() {
        final byte[] pkgjson =
            new TestResource("simple-npm-project/package.json").asBytes();
        final TgzArchive tgz = new TgzArchive(
            Base64.getEncoder().encodeToString(pkgjson)
        );
        MatcherAssert.assertThat(
            tgz.bytes(),
            new IsEqual<>(
                pkgjson
            )
        );
    }

    @Test
    void savesToFile() throws IOException {
        final Path temp = Files.createTempFile("temp", ".tgz");
        new TgzArchive(
            new String(
                new TestResource("binaries/simple-npm-project-1.0.2.tgz").asBytes(),
                StandardCharsets.ISO_8859_1
            ),
            false
        ).saveToFile(temp).blockingGet();
        MatcherAssert.assertThat(
            temp.toFile().exists(),
            new IsEqual<>(true)
        );
    }

    @Test
    void throwsOnMalformedArchive() {
        final TgzArchive tgz = new TgzArchive(
            Base64.getEncoder().encodeToString(
                new byte[]{}
            )
        );
        MatcherAssert.assertThat(
            Assertions.assertThrows(
                ArtipieIOException.class,
                tgz::packageJson
            ),
            new HasPropertyWithValue<>(
                "message",
                new StringContains(
                    "Input is not in the .gz format"
                )
            )
        );
    }

    /**
     * Throws proper exception on empty tgz.
     * {@code tar czvf - --files-from=/dev/null | base64}
     */
    @Test
    void throwsOnMissingFile() {
        final TgzArchive tgz = new TgzArchive(
            "H4sIAAAAAAAAA+3BAQ0AAADCoPdPbQ43oAAAAAAAAAAAAIA3A5reHScAKAAA"
        );
        MatcherAssert.assertThat(
            Assertions.assertThrows(
                ArtipieException.class,
                tgz::packageJson
            ),
            new HasPropertyWithValue<>(
                "message",
                new StringContains(
                    "'package.json' file was not found"
                )
            )
        );
    }

}
