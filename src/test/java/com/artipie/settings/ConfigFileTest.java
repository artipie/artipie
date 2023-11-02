/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings;

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
        final String simple = "filename";
        MatcherAssert.assertThat(
            "Correct name",
            new ConfigFile(String.join("", simple, extension)).name(),
            new IsEqual<>(simple)
        );
        MatcherAssert.assertThat(
            "Correct extension",
            new ConfigFile(String.join("", simple, extension)).extension().orElse(""),
            new IsEqual<>(extension)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {".yaml", ".yml", ".jar", ".json", ""})
    void getFilenameAndExtensionCorrectFromHiddenDir(final String extension) {
        final String name = "..2023_02_06_09_57_10.2284382907/filename";
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

    @ParameterizedTest
    @ValueSource(strings = {".yaml", ".yml", ".jar", ".json", ""})
    void getFilenameAndExtensionCorrectFromHiddenFile(final String extension) {
        final String name = "some_dir/.filename";
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

    @ParameterizedTest
    @ValueSource(strings = {".yaml", ".yml", ".jar", ".json", ""})
    void getFilenameAndExtensionCorrectFromHiddenFileInHiddenDir(final String extension) {
        final String name = ".some_dir/.filename";
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
        "name,false",
        ".some.yaml,true",
        "..hidden_dir/any.yml,true"
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
