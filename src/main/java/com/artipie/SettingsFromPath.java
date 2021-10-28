/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
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
     * example setting set is used.
     * @param port Port for info logging
     * @return Artipie settings
     * @throws IOException On IO error
     */
    public Settings find(final int port) throws IOException {
        boolean initialize = Boolean.parseBoolean(System.getenv("ARTIPIE_INIT"));
        if (!Files.exists(this.path)) {
            new JavaResource("example/artipie.yaml").copy(this.path);
            initialize = true;
        }
        final Settings settings = new YamlSettings(
            Yaml.createYamlInput(this.path.toFile()).readYamlMapping(),
            new SettingsCaches.All()
        );
        final BlockingStorage bsto = new BlockingStorage(settings.storage());
        final Key init = new Key.From(".artipie", "initialized");
        if (initialize && !bsto.exists(init)) {
            final List<String> resources = Arrays.asList(
                "_credentials.yaml", StorageAliases.FILE_NAME, "_permissions.yaml"
            );
            for (final String res : resources) {
                final Path tmp = Files.createTempFile(res, ".tmp");
                new JavaResource(String.format("example/repo/%s", res)).copy(tmp);
                bsto.save(new Key.From(res), Files.readAllBytes(tmp));
                Files.delete(tmp);
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
                    "\t\t\t\t   Check the dashboard at:",
                    String.format(
                        "\t\t\thttp://localhost:%d/dashboard/artipie",
                        port
                    ),
                    "\t-===============================================================-", ""
                )
            );
        }
        return settings;
    }
}
