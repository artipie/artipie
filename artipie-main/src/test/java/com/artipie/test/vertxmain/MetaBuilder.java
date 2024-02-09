/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.test.vertxmain;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import org.apache.hc.core5.net.URIBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Artipie's meta config yaml builder.
 */
public class MetaBuilder {

    private URI baseUrl;

    private Path repos;

    private Path security;

    public MetaBuilder withBaseUrl(URI baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public MetaBuilder withBaseUrl(String host, int port) {
        try {
            this.baseUrl = new URIBuilder()
                    .setScheme("http")
                    .setHost(host)
                    .setPort(port)
                    .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    public MetaBuilder withRepoDir(Path dir) {
        this.repos = Objects.requireNonNull(dir, "Directory cannot be null");
        return this;
    }

    public MetaBuilder withSecurityDir(Path dir) {
        this.security = Objects.requireNonNull(dir, "Directory cannot be null");
        return this;
    }

    public Path build(Path base) throws IOException {
        if (this.repos == null) {
            throw new IllegalStateException("Directory of repositories is not defined");
        }
        if (this.security == null) {
            throw new IllegalStateException("Security directory is not defined");
        }
        YamlMappingBuilder meta = Yaml.createYamlMappingBuilder()
                .add("storage", TestVertxMainBuilder.fileStorageCfg(this.repos));
        if (this.baseUrl != null) {
            meta = meta.add("base_url", this.baseUrl.toString());
        }
        meta = meta.add("credentials",
                Yaml.createYamlSequenceBuilder()
                        .add(
                                Yaml.createYamlMappingBuilder()
                                        .add("type", "artipie")
                                        .build()
                        )
                        .build()
        );
        meta = meta.add("policy",
                Yaml.createYamlMappingBuilder()
                        .add("type", "artipie")
                        .add("storage", TestVertxMainBuilder.fileStorageCfg(this.security))
                        .build());
        String data = Yaml.createYamlMappingBuilder()
                .add("meta", meta.build())
                .build()
                .toString();
        Path res = base.resolve("artipie.yml");
        Files.deleteIfExists(res);
        Files.createFile(res);
        return Files.write(res, data.getBytes());
    }
}
