/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import org.cactoos.list.ListOf;

/**
 * Class for storing maven settings xml.
 * @since 0.12
 */
public final class MavenSettings {
    /**
     * List with settings.
     */
    private final List<String> settings;

    /**
     * Ctor.
     * @param port Port for repository url.
     */
    public MavenSettings(final int port) {
        this.settings = Collections.unmodifiableList(
            new ListOf<String>(
                "<settings>",
                "    <profiles>",
                "        <profile>",
                "            <id>artipie</id>",
                "            <repositories>",
                "                <repository>",
                "                    <id>my-maven</id>",
                String.format("<url>http://host.testcontainers.internal:%d/my-maven/</url>", port),
                "                </repository>",
                "            </repositories>",
                "        </profile>",
                "    </profiles>",
                "    <activeProfiles>",
                "        <activeProfile>artipie</activeProfile>",
                "    </activeProfiles>",
                "</settings>"
            )
        );
    }

    /**
     * Write maven settings to the specified path.
     * @param path Path for writing
     * @throws IOException In case of exception during writing.
     */
    public void writeTo(final Path path) throws IOException {
        Files.write(
            path.resolve("settings.xml"),
            this.settings
        );
    }
}
