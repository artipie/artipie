/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings.repo;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.amihaiemil.eoyaml.YamlSequenceBuilder;
import com.artipie.asto.Content;
import com.artipie.asto.Storage;
import com.artipie.settings.repo.perms.RepoPerms;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Repo config yaml.
 * @since 0.12
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
public final class RepoConfigYaml {

    /**
     * Yaml mapping builder.
     */
    private YamlMappingBuilder builder;

    /**
     * Ctor.
     * @param type Repository type
     */
    public RepoConfigYaml(final String type) {
        this.builder = Yaml.createYamlMappingBuilder().add("type", type);
    }

    /**
     * Adds file storage to config.
     * @param path Path
     * @return Itself
     */
    public RepoConfigYaml withFileStorage(final Path path) {
        this.builder = this.builder.add(
            "storage",
            Yaml.createYamlMappingBuilder()
                .add("type", "fs")
                .add("path", path.toString()).build()
        );
        return this;
    }

    /**
     * Adds alias storage to config.
     * @param alias Storage alias
     * @return Itself
     */
    public RepoConfigYaml withStorageAlias(final String alias) {
        this.builder = this.builder.add("storage", alias);
        return this;
    }

    /**
     * Adds port to config.
     * @param port Port
     * @return Itself
     */
    public RepoConfigYaml withPort(final int port) {
        this.builder = this.builder.add("port", String.valueOf(port));
        return this;
    }

    /**
     * Adds url to config.
     * @param url Url
     * @return Itself
     */
    public RepoConfigYaml withUrl(final String url) {
        this.builder = this.builder.add("url", url);
        return this;
    }

    /**
     * Adds path to config.
     * @param path Path
     * @return Itself
     */
    public RepoConfigYaml withPath(final String path) {
        this.builder = this.builder.add("path", path);
        return this;
    }

    /**
     * Adds remote in settings section to config.
     * @param url URL
     * @return Itself
     */
    public RepoConfigYaml withRemoteSettings(final String url) {
        this.builder = this.builder.add(
            "settings",
                Yaml.createYamlMappingBuilder().add(
                    "remote",
                    Yaml.createYamlMappingBuilder().add(
                        "url", url
                    ).build()
                ).build()
        );
        return this;
    }

    /**
     * Adds permissions section to config.
     * @param perms Permissions
     * @return Itself
     */
    public RepoConfigYaml withPermissions(final RepoPerms perms) {
        this.builder = this.builder.add("permissions", perms.permsYaml())
            .add("permissions_include_patterns", perms.patternsYaml());
        return this;
    }

    /**
     * Adds remote to config.
     * @param url URL
     * @return Itself
     */
    public RepoConfigYaml withRemote(final String url) {
        this.builder = this.builder.add(
            "remotes",
            Yaml.createYamlSequenceBuilder().add(
                Yaml.createYamlMappingBuilder().add("url", url).build()
            ).build()
        );
        return this;
    }

    /**
     * Adds remotes to config.
     * @param remotes Remotes yaml sequence
     * @return Itself
     */
    public RepoConfigYaml withRemotes(final YamlSequenceBuilder remotes) {
        this.builder = this.builder.add("remotes", remotes.build());
        return this;
    }

    /**
     * Adds remote with authentication to config.
     * @param url URL
     * @param username Username
     * @param password Password
     * @return Itself
     */
    public RepoConfigYaml withRemote(
        final String url,
        final String username,
        final String password
    ) {
        this.builder = this.builder.add(
            "remotes",
            Yaml.createYamlSequenceBuilder().add(
                Yaml.createYamlMappingBuilder()
                    .add("url", url)
                    .add("username", username)
                    .add("password", password)
                    .build()
            ).build()
        );
        return this;
    }

    /**
     * Adds Components and Architectures.
     * @param components Components space separated list
     * @param archs Architectures space separated list
     * @return Itself
     */
    public RepoConfigYaml withComponentsAndArchs(final String components, final String archs) {
        this.builder = this.builder.add(
            "settings",
            Yaml.createYamlMappingBuilder()
                .add("Components", components).add("Architectures", archs).build()
        );
        return this;
    }

    /**
     * Saves repo config to the provided storage with given name.
     * @param storage Where to save
     * @param name Name to save with
     */
    public void saveTo(final Storage storage, final String name) {
        storage.save(ConfigFile.Extension.YAML.key(name), this.toContent()).join();
    }

    /**
     * Repo config as yaml mapping.
     * @return Instance of {@link YamlMapping}
     */
    public YamlMapping yaml() {
        return Yaml.createYamlMappingBuilder().add("repo", this.builder.build()).build();
    }

    @Override
    public String toString() {
        return this.yaml().toString();
    }

    /**
     * Repo settings as content.
     * @return Instanse of {@link Content}
     */
    public Content toContent() {
        return new Content.From(this.toString().getBytes(StandardCharsets.UTF_8));
    }
}
