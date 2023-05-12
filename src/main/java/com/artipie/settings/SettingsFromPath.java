/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.VertxMain;
import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.misc.JavaResource;
import com.jcabi.log.Logger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Obtain artipie settings by path.
 * @since 0.22
 */
public final class SettingsFromPath {

    /**
     * Path to find setting by.
     */
    private final Path path;

    /**
     * Ctor.
     * @param path Path to find setting by
     */
    public SettingsFromPath(final Path path) {
        this.path = path;
    }

    /**
     * Searches settings by the provided path, if no settings are found,
     * example settings are used.
     * @return Artipie settings
     * @throws IOException On IO error
     */
    public Settings find() throws IOException {
        boolean initialize = Boolean.parseBoolean(System.getenv("ARTIPIE_INIT"));
        if (!Files.exists(this.path)) {
            new JavaResource("example/artipie.yaml").copy(this.path);
            initialize = true;
        }
        final Settings settings = new YamlSettings(
            Yaml.createYamlInput(this.path.toFile()).readYamlMapping()
        );
        final BlockingStorage bsto = new BlockingStorage(settings.configStorage());
        final Key init = new Key.From(".artipie", "initialized");
        if (initialize && !bsto.exists(init)) {
            SettingsFromPath.copyResources(
                Arrays.asList(
                    AliasSettings.FILE_NAME, "my-bin.yaml", "my-docker.yaml", "my-maven.yaml"
                ), "repo", bsto
            );
            if (settings.authz().policyStorage().isPresent()) {
                final BlockingStorage policy = new BlockingStorage(
                    settings.authz().policyStorage().get()
                );
                SettingsFromPath.copyResources(
                    Arrays.asList(
                        "roles/reader.yml", "roles/default/github.yml", "roles/api-admin.yaml",
                        "users/artipie.yaml"
                    ), "security", policy
                );
            }
            bsto.save(init, "true".getBytes());
            Logger.info(
                VertxMain.class,
                String.join(
                    "\n",
                    "", "", "\t+===============================================================+",
                    "\t\t\t\t\tHello!",
                    "\t\tArtipie configuration was not found, created default.",
                    "\t\t\tDefault username/password: `artipie`/`artipie`. ",
                    "\t-===============================================================-", ""
                )
            );
        }
        return settings;
    }

    /**
     * Copies given resources list from given directory to the blocking storage.
     * @param resources What to copy
     * @param dir Example resources directory
     * @param bsto Where to copy
     * @throws IOException On error
     */
    private static void copyResources(
        final List<String> resources, final String dir, final BlockingStorage bsto
    ) throws IOException {
        for (final String res : resources) {
            final Path tmp = Files.createTempFile(
                Path.of(res).getFileName().toString(), ".tmp"
            );
            new JavaResource(String.format("example/%s/%s", dir, res)).copy(tmp);
            bsto.save(new Key.From(res), Files.readAllBytes(tmp));
            Files.delete(tmp);
        }
    }
}
