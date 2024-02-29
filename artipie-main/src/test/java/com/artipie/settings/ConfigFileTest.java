/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.settings;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

/**
 * Test cases for {@link ConfigFile}.
 */
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
        Assertions.assertTrue(new ConfigFile(new Key.From(ConfigFileTest.NAME + extension))
            .existsIn(storage)
            .toCompletableFuture().join());
    }

    @ParameterizedTest
    @ValueSource(strings = {".yaml", ".yml", ""})
    void valueFromStorageReturnsContentWhenYamlExist(final String extension) {
        final Storage storage = new InMemoryStorage();
        this.saveByKey(storage, ".yml");
        Assertions.assertArrayEquals(
            ConfigFileTest.CONTENT,
            new ConfigFile(new Key.From(ConfigFileTest.NAME + extension))
                .valueFrom(storage).toCompletableFuture().join().asBytes()
        );
    }

    @Test
    void valueFromStorageReturnsYamlWhenBothExist() {
        final Storage storage = new InMemoryStorage();
        final String yaml = String.join("", Arrays.toString(ConfigFileTest.CONTENT), "some");
        this.saveByKey(storage, ".yml");
        this.saveByKey(storage, ".yaml", yaml.getBytes());
        Assertions.assertEquals(
            yaml,
            new ConfigFile(new Key.From(ConfigFileTest.NAME))
                .valueFrom(storage)
                .toCompletableFuture().join().asString()
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {".yaml", ".yml", ".jar", ".json", ""})
    void getFilenameAndExtensionCorrect(final String extension) {
        final String simple = "filename";
        Assertions.assertEquals(simple,
            new ConfigFile(simple + extension).name(), "Correct name");
        Assertions.assertEquals(extension,
            new ConfigFile(simple + extension).extension().orElse(""), "Correct extension");
    }

    @ParameterizedTest
    @ValueSource(strings = {".yaml", ".yml", ".jar", ".json", ""})
    void getFilenameAndExtensionCorrectFromHiddenDir(final String extension) {
        final String name = "..2023_02_06_09_57_10.2284382907/filename";
        Assertions.assertEquals(name,
            new ConfigFile(name + extension).name(), "Correct name");
        Assertions.assertEquals(extension,
            new ConfigFile(name + extension).extension().orElse(""), "Correct extension");
    }

    @ParameterizedTest
    @ValueSource(strings = {".yaml", ".yml", ".jar", ".json", ""})
    void getFilenameAndExtensionCorrectFromHiddenFile(final String extension) {
        final String name = "some_dir/.filename";
        Assertions.assertEquals(name, new ConfigFile(name + extension).name(), "Correct name");
        Assertions.assertEquals(extension,
            new ConfigFile(name + extension).extension().orElse(""), "Correct extension");
    }

    @ParameterizedTest
    @ValueSource(strings = {".yaml", ".yml", ".jar", ".json", ""})
    void getFilenameAndExtensionCorrectFromHiddenFileInHiddenDir(final String extension) {
        final String name = ".some_dir/.filename";
        Assertions.assertEquals(name, new ConfigFile(name + extension).name(), "Correct name");
        Assertions.assertEquals(extension, new ConfigFile(name + extension).extension().orElse(""),
            "Correct extension");
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
        Assertions.assertFalse(
            new ConfigFile("filename.jar")
                .existsIn(new InMemoryStorage())
                .toCompletableFuture().join()
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
        Assertions.assertEquals(yaml, new ConfigFile(filename).isYamlOrYml());
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
