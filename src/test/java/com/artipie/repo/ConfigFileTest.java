/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie.repo;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import java.util.Arrays;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test cases for {@link ConfigFile}.
 * @since 0.14
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class ConfigFileTest {

    /**
     * Filename.
     */
    private static final String NAME = "my-file";

    /**
     * Content.
     */
    private static final byte[] CONTENT = "content from config file".getBytes();

    @ParameterizedTest
    @ValueSource(strings = {".yaml", ".yml", ""})
    void existInStorageReturnsTrueWhenYamlExist(final String extension) {
        final Storage storage = new InMemoryStorage();
        this.saveByKey(storage, ".yaml");
        MatcherAssert.assertThat(
            new ConfigFile(new Key.From(ConfigFileTest.NAME + extension))
                .existsIn(storage)
                .toCompletableFuture().join(),
            new IsEqual<>(true)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {".yaml", ".yml", ""})
    void valueFromStorageReturnsContentWhenYamlExist(final String extension) {
        final Storage storage = new InMemoryStorage();
        this.saveByKey(storage, ".yml");
        MatcherAssert.assertThat(
            new PublisherAs(
                new ConfigFile(new Key.From(ConfigFileTest.NAME + extension))
                    .valueFrom(storage)
                    .toCompletableFuture().join()
            ).bytes()
            .toCompletableFuture().join(),
            new IsEqual<>(ConfigFileTest.CONTENT)
        );
    }

    @Test
    void valueFromStorageReturnsYamlWhenBothExist() {
        final Storage storage = new InMemoryStorage();
        final String yaml = String.join("", Arrays.toString(ConfigFileTest.CONTENT), "some");
        this.saveByKey(storage, ".yml");
        this.saveByKey(storage, ".yaml", yaml.getBytes());
        MatcherAssert.assertThat(
            new PublisherAs(
                new ConfigFile(new Key.From(ConfigFileTest.NAME))
                    .valueFrom(storage)
                    .toCompletableFuture().join()
            ).asciiString()
            .toCompletableFuture().join(),
            new IsEqual<>(yaml)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {".yaml", ".yml", ".jar", ".json", ""})
    void getFilenameAndExtensionCorrect(final String extension) {
        final String name = "filename";
        MatcherAssert.assertThat(
            "Correct name",
            new ConfigFile(String.join("", name, extension)).name(),
            new IsEqual<>(name)
        );
        MatcherAssert.assertThat(
            "Correct extension",
            new ConfigFile(String.join("", name, extension)).extension().orElse(""),
            new IsEqual<>(extension)
        );
    }

    @Test
    void failsGetNameFromEmptyString() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new ConfigFile("").name()
        );
    }

    @Test
    void valueFromFailsForNotYamlOrYmlOrWithoutExtensionFiles() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new ConfigFile("name.json").valueFrom(new InMemoryStorage())
        );
    }

    @Test
    void returnFalseForConfigFileWithBadExtension() {
        MatcherAssert.assertThat(
            new ConfigFile("filename.jar")
                .existsIn(new InMemoryStorage())
                .toCompletableFuture().join(),
            new IsEqual<>(false)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "file.yaml,true",
        "name.yml,true",
        "name.xml,false",
        "name,false"
    })
    void yamlOrYmlDeterminedCorrectly(final String filename, final boolean yaml) {
        MatcherAssert.assertThat(
            new ConfigFile(filename)
                .isYamlOrYml(),
            new IsEqual<>(yaml)
        );
    }

    private void saveByKey(final Storage storage, final String extension) {
        this.saveByKey(storage, extension, ConfigFileTest.CONTENT);
    }

    private void saveByKey(final Storage storage, final String extension, final byte[] content) {
        storage.save(
            new Key.From(String.format("%s%s", ConfigFileTest.NAME, extension)),
            new Content.From(content)
        );
    }

}
