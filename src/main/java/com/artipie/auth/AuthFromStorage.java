/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.auth;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.http.auth.Authentication;
import com.jcabi.log.Logger;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * Authentication from abstract storage.
 * The storage is expected to have yaml files with credentials in the following structure:
 * <pre>
 * ..
 * ├── users
 * │   ├── david.yaml
 * │   ├── jane.yml
 * │   ├── ...
 * </pre>
 * where the name of the file is username (case-sensitive), both yml and yaml extensions are
 * supported. The yaml format file is the following:
 * <pre>{@code
 * david:
 *   type: plain # plain and sha256 types are supported
 *   pass: qwerty
 *   email: david@example.com # Optional
 *   enabled: true # optional default true
 *   roles:
 *     - java-dev
 *   permissions:
 *     artipie_basic_permission:
 *       rpm-repo:
 *         - read
 * }</pre>
 * @since 1.29
 */
public final class AuthFromStorage implements Authentication {

    /**
     * The storage to obtain users files from.
     */
    private final BlockingStorage asto;

    /**
     * Ctor.
     * @param asto Abstract blocking storage
     */
    public AuthFromStorage(final BlockingStorage asto) {
        this.asto = asto;
    }

    @Override
    public Optional<User> user(final String name, final String pass) {
        final Optional<byte[]> res;
        final Key yaml = new Key.From(String.format("users/%s.yaml", name));
        final Key yml = new Key.From(String.format("users/%s.yml", name));
        if (this.asto.exists(yaml)) {
            res = Optional.of(this.asto.value(yaml));
        } else if (this.asto.exists(yml)) {
            res = Optional.of(this.asto.value(yml));
        } else {
            res = Optional.empty();
        }
        return res.map(bytes -> AuthFromStorage.readAndCheckFromYaml(bytes, name, pass))
            .flatMap(opt -> opt);
    }

    /**
     * Reads bytes as yaml and check the password.
     * @param bytes Yaml bytes
     * @param name Username
     * @param pass Password to check
     * @return User if yaml parsed and password is correct
     */
    private static Optional<User> readAndCheckFromYaml(final byte[] bytes, final String name,
        final String pass) {
        Optional<User> res = Optional.empty();
        try {
            final YamlMapping yaml = Yaml.createYamlInput(new ByteArrayInputStream(bytes))
                .readYamlMapping();
            final YamlMapping info = yaml.yamlMapping(name);
            if (info != null
                && !Boolean.FALSE.toString().equalsIgnoreCase(info.string("enabled"))) {
                final String type = info.string("type");
                final String origin = info.string("pass");
                if ("plain".equals(type) && Objects.equals(origin, pass)) {
                    res = Optional.of(new User(name));
                } else if ("sha256".equals(type) && DigestUtils.sha256Hex(pass).equals(origin)) {
                    res = Optional.of(new User(name));
                }
            }
        } catch (final IOException err) {
            Logger.error(AuthFromStorage.class, "Failed to parse yaml for user %s", name);
        }
        return res;
    }
}
