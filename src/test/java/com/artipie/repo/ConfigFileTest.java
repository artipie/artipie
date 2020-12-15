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
import com.artipie.asto.memory.InMemoryStorage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test cases for {@link ConfigFile}.
 * @since 0.14
 */
final class ConfigFileTest {

    /**
     * Filename.
     */
    private static final String NAME = "my-file";

    @ParameterizedTest
    @CsvSource({".yaml", ".yml", "''"})
    void existInStorageReturnsWhenYamlExist(final String extension) {
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
    @CsvSource({".yaml", ".yml", ".jar", ".json", "''"})
    void getFilenameWithoutExtensionCorrect(final String extension) {
        final String name = "filename";
        MatcherAssert.assertThat(
            new ConfigFile(String.join("", name, extension)).name(),
            new IsEqual<>(name)
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
    void failToCheckExistingForConfigFileWithBadExtension() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> new ConfigFile("filename.jar").existsIn(new InMemoryStorage())
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
        storage.save(
            new Key.From(String.format("%s%s", ConfigFileTest.NAME, extension)),
            Content.EMPTY
        );
    }

}
