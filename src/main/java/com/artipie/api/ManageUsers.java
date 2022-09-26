/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.amihaiemil.eoyaml.YamlNode;
import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.misc.Json2Yaml;
import com.artipie.misc.Yaml2Json;
import com.artipie.settings.YamlSettings;
import com.artipie.settings.users.CrudUsers;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 * Users from yaml file.
 *
 * @since 0.1
 */
public final class ManageUsers implements CrudUsers {

    /**
     * Yaml field name for password.
     */
    private static final String PASS = "pass";

    /**
     * Yaml field name for password type.
     */
    private static final String TYPE = "type";

    /**
     * Yaml file key.
     */
    private final Key key;

    /**
     * Storage.
     */
    private final BlockingStorage blsto;

    /**
     * Ctor.
     *
     * @param key Yaml file key
     * @param blsto Storage
     */
    public ManageUsers(final Key key, final BlockingStorage blsto) {
        this.key = key;
        this.blsto = blsto;
    }

    @Override
    public JsonArray list() {
        final Optional<YamlMapping> users = this.users();
        final JsonArrayBuilder builder = Json.createArrayBuilder();
        users.map(
            yaml -> yaml.keys().stream().map(node -> node.asScalar().value()).map(
                name -> jsonFromYaml(name, users.get().yamlMapping(name))
            ).collect(Collectors.toList())
        ).orElse(Collections.emptyList()).forEach(builder::add);
        return builder.build();
    }

    @Override
    public Optional<JsonObject> get(final String uname) {
        return this.users().flatMap(yaml -> Optional.ofNullable(yaml.yamlMapping(uname)))
            .map(yaml -> jsonFromYaml(uname, yaml));
    }

    @Override
    public void addOrUpdate(final JsonObject info, final String uname) {
        YamlMappingBuilder builder = Yaml.createYamlMappingBuilder();
        final Optional<YamlMapping> users = this.users();
        if (users.isPresent()) {
            for (final YamlNode node : users.get().keys()) {
                final String val = node.asScalar().value();
                builder = builder.add(val, users.get().yamlMapping(val));
            }
        }
        builder = builder.add(uname, new Json2Yaml().apply(info.toString()));
        this.blsto.save(
            this.key,
            Yaml.createYamlMappingBuilder().add(YamlSettings.NODE_CREDENTIALS, builder.build())
                .build().toString().getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    public void remove(final String uname) {
        if (this.users().map(yaml -> yaml.yamlMapping(uname) != null).orElse(false)) {
            YamlMappingBuilder builder = Yaml.createYamlMappingBuilder();
            final YamlMapping users = this.users().get();
            for (final YamlNode node : users.keys()) {
                final String val = node.asScalar().value();
                if (!uname.equals(val)) {
                    builder = builder.add(val, users.yamlMapping(val));
                }
            }
            this.blsto.save(
                this.key,
                Yaml.createYamlMappingBuilder()
                    .add(YamlSettings.NODE_CREDENTIALS, builder.build())
                    .build().toString().getBytes(StandardCharsets.UTF_8)
            );
            return;
        }
        throw new IllegalStateException(String.format("User %s does not exist", uname));
    }

    @Override
    public void alterPassword(final String uname, final JsonObject info) {
        if (this.users().map(yaml -> yaml.yamlMapping(uname) != null).orElse(false)) {
            YamlMappingBuilder all = Yaml.createYamlMappingBuilder();
            final YamlMapping users = this.users().get();
            for (final YamlNode node : users.keys()) {
                final String val = node.asScalar().value();
                if (!uname.equals(val)) {
                    all = all.add(val, users.yamlMapping(val));
                }
            }
            final YamlMapping user = this.users().get().yamlMapping(uname);
            YamlMappingBuilder changing = Yaml.createYamlMappingBuilder();
            for (final YamlNode node : user.keys()) {
                final String val = node.asScalar().value();
                if (!ManageUsers.TYPE.equals(val) && !ManageUsers.PASS.equals(val)) {
                    changing = changing.add(val, user.value(val));
                }
            }
            changing = changing.add(ManageUsers.PASS, info.getString("new_pass"));
            changing = changing.add(ManageUsers.TYPE, info.getString("new_type"));
            all = all.add(uname, changing.build());
            this.blsto.save(
                this.key,
                Yaml.createYamlMappingBuilder()
                    .add(YamlSettings.NODE_CREDENTIALS, all.build())
                    .build().toString().getBytes(StandardCharsets.UTF_8)
            );
            return;
        }
        throw new IllegalStateException(String.format("User %s does not found", uname));
    }

    /**
     * Read yaml mapping with users from yaml file.
     *
     * @return Users yaml mapping
     */
    private Optional<YamlMapping> users() {
        Optional<YamlMapping> res = Optional.empty();
        if (this.blsto.exists(this.key)) {
            try {
                res = Optional.ofNullable(
                    Yaml.createYamlInput(
                        new String(this.blsto.value(this.key), StandardCharsets.UTF_8)
                    ).readYamlMapping().yamlMapping("credentials")
                );
            } catch (final IOException err) {
                throw new UncheckedIOException(err);
            }
        }
        return res;
    }

    /**
     * Transform user info in yaml format to json.
     * @param uname User name
     * @param yaml Yaml info
     * @return User info as json object
     */
    private static JsonObject jsonFromYaml(final String uname, final YamlMapping yaml) {
        final JsonObjectBuilder usr = Json.createObjectBuilder(
            new Yaml2Json().apply(yaml.toString()).asJsonObject()
        );
        usr.remove(ManageUsers.PASS);
        usr.remove(ManageUsers.TYPE);
        return Json.createObjectBuilder().add("name", uname).addAll(usr).build();
    }
}
