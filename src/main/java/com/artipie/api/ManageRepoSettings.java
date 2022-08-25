/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.api;

import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.settings.StorageAliases;
import com.artipie.settings.repo.CrudRepoSettings;
import java.util.ArrayList;
import java.util.Collection;
import javax.json.Json;
import javax.json.JsonStructure;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Manage repository settings.
 * @since 0.26
 */
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
    public boolean exists(final String name) {
        throw ManageRepoSettings.NOT_IMPLEMENTED;
    }

    @Override
    public JsonStructure value(final String name) {
        return Json.createObjectBuilder().add("type", "maven").add("storage", "def").build();
    }

    @Override
    public void save(final String name, final JsonStructure value) {
        throw ManageRepoSettings.NOT_IMPLEMENTED;
    }

    @Override
    public void delete(final String name) {
        throw ManageRepoSettings.NOT_IMPLEMENTED;
    }

    @Override
    public void move(final String name, final String nname) {
        throw ManageRepoSettings.NOT_IMPLEMENTED;
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
            // @checkstyle BooleanExpressionComplexityCheck (5 lines)
            if ((name.endsWith(".yaml") || name.endsWith(".yml"))
                && !name.contains(StorageAliases.FILE_NAME)
                && !name.contains("_permissions")
                && !name.contains("_credentials")) {
                res.add(name.replaceAll("\\.yaml|\\.yml", ""));
            }
        }
        return res;
    }
}
