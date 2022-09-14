/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.misc.Json2Yaml;
import com.artipie.misc.Yaml2Json;
import com.artipie.settings.repo.CrudRepoSettings;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import javax.json.JsonStructure;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Manage repository settings.
 * @since 0.26
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class ManageRepoSettings implements CrudRepoSettings {
    /**
     * Not implemented error.
     */
    private static final NotImplementedException NOT_IMPLEMENTED =
        new NotImplementedException("Not implemented yet");

    /**
     * Repository settings storage.
     */
    private final BlockingStorage asto;

    /**
     * Ctor.
     * @param asto Repository settings storage
     */
    public ManageRepoSettings(final BlockingStorage asto) {
        this.asto = asto;
    }

    @Override
    public Collection<String> listAll() {
        return this.list(Key.ROOT);
    }

    @Override
    public Collection<String> list(final String uname) {
        return this.list(new Key.From(uname));
    }

    @Override
    public boolean exists(final RepositoryName rname) {
        final ConfigKeys keys = new ConfigKeys(rname.toString());
        return this.asto.exists(keys.yamlKey()) || this.asto.exists(keys.ymlKey());
    }

    @Override
    public JsonStructure value(final RepositoryName rname) {
        final ConfigKeys keys = new ConfigKeys(rname.toString());
        JsonStructure json = null;
        if (this.asto.exists(keys.yamlKey())) {
            json = new Yaml2Json().apply(
                new String(
                    this.asto.value(keys.yamlKey()),
                    StandardCharsets.UTF_8
                )
            ).asJsonObject();
        } else if (this.asto.exists(keys.ymlKey())) {
            json = new Yaml2Json().apply(
                new String(
                    this.asto.value(keys.ymlKey()),
                    StandardCharsets.UTF_8
                )
            ).asJsonObject().getJsonObject(BaseRest.REPO);
        }
        return json;
    }

    @Override
    public void save(final RepositoryName rname, final JsonStructure value) {
        final ConfigKeys keys = new ConfigKeys(rname.toString());
        this.asto.save(
            keys.yamlKey(),
            new Json2Yaml().apply(value.toString()).toString().getBytes(StandardCharsets.UTF_8)
        );
    }

    @Override
    public void delete(final RepositoryName rname) {
        new ConfigKeys(rname.toString()).keys()
            .forEach(
                key -> {
                    if (this.asto.exists(key)) {
                        this.asto.delete(key);
                    }
                }
            );
    }

    @Override
    public void move(final String name, final String nname) {
        throw ManageRepoSettings.NOT_IMPLEMENTED;
    }

    @Override
    public boolean hasSettingsDuplicates(final RepositoryName rname) {
        final ConfigKeys keys = new ConfigKeys(rname.toString());
        return this.asto.exists(keys.yamlKey()) && this.asto.exists(keys.ymlKey());
    }

    /**
     * List existing repositories.
     * @param key Key (ROOT or user name)
     * @return List of the repositories
     */
    private Collection<String> list(final Key key) {
        final Collection<String> res = new ArrayList<>(5);
        for (final Key item : this.asto.list(key)) {
            final String name = item.string();
            if (yamlFilename(name) && new RepositoryNameValidator(name).valid()) {
                res.add(name.replaceAll("\\.yaml|\\.yml", ""));
            }
        }
        return res;
    }

    /**
     * Checks whether name is yaml-file name.
     * @param name Key name
     * @return True if name has yaml-file extension
     */
    private static boolean yamlFilename(final String name) {
        return name.endsWith(".yaml") || name.endsWith(".yml");
    }
}
