/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlInput;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.amihaiemil.eoyaml.YamlNode;
import com.amihaiemil.eoyaml.YamlSequenceBuilder;
import com.artipie.asto.Concatenation;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Remaining;
import com.artipie.asto.Storage;
import com.artipie.auth.AuthFromYaml;
import com.artipie.http.auth.Authentication;
import com.artipie.management.Users;
import com.artipie.repo.ConfigFile;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * Credentials from main artipie config.
 * @since 0.12.2
 */
public final class UsersFromStorageYaml implements Users {

    /**
     * Credentials yaml mapping key.
     */
    private static final String CREDENTIALS = "credentials";

    /**
     * Credentials yaml mapping key.
     */
    private static final String EMAIL = "email";

    /**
     * Groups yaml mapping key.
     */
    private static final String GROUPS = "groups";

    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * Credentials key.
     */
    private final Key key;

    /**
     * Ctor.
     * @param storage Storage
     * @param key Credentials key
     */
    public UsersFromStorageYaml(final Storage storage, final Key key) {
        this.storage = storage;
        this.key = key;
    }

    @Override
    public CompletionStage<List<User>> list() {
        return this.yaml().thenApply(
            yaml -> yaml.yamlMapping(UsersFromStorageYaml.CREDENTIALS).keys().stream()
                .map(
                    node -> {
                        final String name = node.asScalar().value();
                        return new User(
                            name,
                            Optional.ofNullable(
                                yaml.yamlMapping(UsersFromStorageYaml.CREDENTIALS)
                                    .yamlMapping(name).string(UsersFromStorageYaml.EMAIL)
                            ),
                            Optional.ofNullable(
                                yaml.yamlMapping(UsersFromStorageYaml.CREDENTIALS).yamlMapping(name)
                                .yamlSequence(UsersFromStorageYaml.GROUPS)
                            ).map(
                                groups -> groups.values().stream()
                                    .map(item -> item.asScalar().value())
                                    .collect(Collectors.toSet())
                            ).orElse(Collections.emptySet())
                        );
                    }
                ).collect(Collectors.toList())
        );
    }

    @Override
    public CompletionStage<Void> add(final User user, final String pswd,
        final PasswordFormat format) {
        return this.yaml().thenCompose(
            yaml -> {
                YamlMappingBuilder result = UsersFromStorageYaml
                    .removeUserRecord(user.name(), yaml);
                YamlMappingBuilder info = Yaml.createYamlMappingBuilder()
                    .add("type", format.name().toLowerCase(Locale.US))
                    .add("pass", pswd);
                if (user.email().isPresent()) {
                    info = info.add(UsersFromStorageYaml.EMAIL, user.email().get());
                }
                if (!user.groups().isEmpty()) {
                    YamlSequenceBuilder seq = Yaml.createYamlSequenceBuilder();
                    for (final String group : user.groups()) {
                        seq = seq.add(group);
                    }
                    info = info.add(UsersFromStorageYaml.GROUPS, seq.build());
                }
                result = result.add(user.name(), info.build());
                return this.buildAndSaveCredentials(result);
            }
        );
    }

    @Override
    public CompletionStage<Void> remove(final String username) {
        return this.yaml().thenCompose(
            yaml -> this.buildAndSaveCredentials(
                UsersFromStorageYaml.removeUserRecord(username, yaml)
            )
        );
    }

    @Override
    public CompletionStage<Authentication> auth() {
        return this.yaml().thenApply(AuthFromYaml::new);
    }

    /**
     * Credentials as yaml.
     * @return Completion action with yaml
     * @todo #730:30min When `ContentAs` is used here instead of `Concatenation` and
     *  `Remaining` ITs get stuck on github actions (this does not happen locally on mac os),
     *  figure out why, make necessary corrections and use `ContentAs` here.
     */
    public CompletionStage<YamlMapping> yaml() {
        return SingleInterop.fromFuture(
            new ConfigFile(this.key).valueFrom(this.storage)
        ).flatMap(content -> new Concatenation(content).single())
        .map(Remaining::new)
        .map(Remaining::bytes)
        .map(bytes -> Yaml.createYamlInput(new String(bytes, StandardCharsets.US_ASCII)))
        .map(YamlInput::readYamlMapping)
        .to(SingleInterop.get());
    }

    /**
     * Build credentials yaml from users yaml mapping and saves it to storage.
     * @param users Users mapping
     * @return Credentials yaml string representation
     */
    private CompletionStage<Void> buildAndSaveCredentials(final YamlMappingBuilder users) {
        return this.storage.save(
            this.key,
            new Content.From(
                Yaml.createYamlMappingBuilder()
                    .add(UsersFromStorageYaml.CREDENTIALS, users.build()).build()
                    .toString().getBytes(StandardCharsets.UTF_8)
            )
        );
    }

    /**
     * Removes user record from credentials.yaml.
     * @param username User name to remove
     * @param yaml Credentials mapping
     * @return YamlMappingBuilder without removed user
     */
    private static YamlMappingBuilder removeUserRecord(final String username,
        final YamlMapping yaml) {
        YamlMappingBuilder result = Yaml.createYamlMappingBuilder();
        final YamlMapping credentials = yaml.yamlMapping(UsersFromStorageYaml.CREDENTIALS);
        final List<YamlNode> keep = credentials.keys().stream()
            .filter(node -> !node.asScalar().value().equals(username))
            .collect(Collectors.toList());
        for (final YamlNode node : keep) {
            result = result.add(node, credentials.value(node));
        }
        return result;
    }
}
