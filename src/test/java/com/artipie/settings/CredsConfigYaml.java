/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.settings;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.amihaiemil.eoyaml.YamlSequenceBuilder;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.settings.users.Users;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * Credentials config yaml.
 * @since 0.12
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class CredsConfigYaml {

    /**
     * Yaml mapping builder.
     */
    private YamlMappingBuilder builder;

    /**
     * Ctor.
     */
    public CredsConfigYaml() {
        this.builder = Yaml.createYamlMappingBuilder();
    }

    /**
     * Adds users to credentials config.
     * @param usernames Names to add
     * @return Itself
     */
    public CredsConfigYaml withUsers(final String... usernames) {
        for (final String name : usernames) {
            this.builder = this.builder.add(
                name, Yaml.createYamlMappingBuilder()
                    .add("pass", "123")
                    .add("type", "plain").build()
            );
        }
        return this;
    }

    /**
     * Adds user with groups to credentials config.
     * @param username Name of the user
     * @param groups Groups list
     * @return Itself
     */
    public CredsConfigYaml withUserAndGroups(final String username, final List<String> groups) {
        YamlMappingBuilder user = Yaml.createYamlMappingBuilder()
            .add("pass", "123")
            .add("type", "plain");
        if (!groups.isEmpty()) {
            YamlSequenceBuilder seq = Yaml.createYamlSequenceBuilder();
            for (final String group : groups) {
                seq = seq.add(group);
            }
            user = user.add("groups", seq.build());
        }
        this.builder = this.builder.add(username, user.build());
        return this;
    }

    /**
     * Adds user with plain password to credentials config.
     * @param username Name of the user
     * @param pswd Password
     * @return Itself
     */
    public CredsConfigYaml withUserAndPlainPswd(final String username, final String pswd) {
        this.builder = this.builder.add(
            username,
            Yaml.createYamlMappingBuilder()
                .add("type", "plain")
                .add("pass", pswd).build()
        );
        return this;
    }

    /**
     * Adds user with password to credentials config. For sha256 format password is calculated.
     * @param username Name of the user
     * @param format Password format
     * @param pswd Password
     * @return Itself
     */
    public CredsConfigYaml withUserAndPswd(final String username, final Users.PasswordFormat format,
        final String pswd) {
        String pass = pswd;
        if (format == Users.PasswordFormat.SHA256) {
            pass = DigestUtils.sha256Hex(pass);
        }
        this.builder = this.builder.add(
            username,
            Yaml.createYamlMappingBuilder()
                .add("type", format.name().toLowerCase(Locale.US))
                .add("pass", pass).build()
        );
        return this;
    }

    /**
     * Adds user with full info: name, password, email and groups.
     * @param username Name
     * @param format Password format
     * @param pswd Password
     * @param email Email
     * @param groups Groups
     * @return Itself
     * @checkstyle ParameterNumberCheck (500 lines)
     */
    public CredsConfigYaml withFullInfo(final
        String username, final Users.PasswordFormat format,
        final String pswd, final String email, final Set<String> groups) {
        String pass = pswd;
        if (format == Users.PasswordFormat.SHA256) {
            pass = DigestUtils.sha256Hex(pass);
        }
        YamlMappingBuilder user = Yaml.createYamlMappingBuilder()
            .add("type", format.name().toLowerCase(Locale.US))
            .add("pass", pass)
            .add("email", email);
        if (!groups.isEmpty()) {
            YamlSequenceBuilder seq = Yaml.createYamlSequenceBuilder();
            for (final String group : groups) {
                seq = seq.add(group);
            }
            user = user.add("groups", seq.build());
        }
        this.builder = this.builder.add(username, user.build());
        return this;
    }

    /**
     * Saves credentials config to the provided storage by provided name.
     * @param storage Where to save
     * @param key Storage item name
     */
    public void saveTo(final Storage storage, final Key key) {
        storage.save(key, new Content.From(this.toString().getBytes())).join();
    }

    /**
     * Saves credentials config to the provided storage by `_credentials.yaml` name.
     * @param storage Where to save
     */
    public void saveTo(final Storage storage) {
        this.saveTo(storage, new Key.From("_credentials.yaml"));
    }

    /**
     * Credentials config as yaml mapping.
     * @return Instance of {@link YamlMapping}
     */
    public YamlMapping yaml() {
        return Yaml.createYamlMappingBuilder().add("credentials", this.builder.build()).build();
    }

    @Override
    public String toString() {
        return this.yaml().toString();
    }
}
