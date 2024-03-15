/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.factory;

import com.artipie.ArtipieException;
import com.artipie.asto.Storage;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Storages to get instance of storage.
 */
public final class StoragesLoader
    extends FactoryLoader<StorageFactory, ArtipieStorageFactory, Config, Storage> {

    public static StoragesLoader STORAGES = new StoragesLoader();

    /**
     * Environment parameter to define packages to find storage factories.
     * Package names should be separated by semicolon ';'.
     */
    public static final String SCAN_PACK = "STORAGE_FACTORY_SCAN_PACKAGES";

    /**
     * Ctor.
     */
    private StoragesLoader() {
        this(System.getenv());
    }

    /**
     * Ctor.
     *
     * @param env Environment parameters.
     */
    public StoragesLoader(final Map<String, String> env) {
        super(ArtipieStorageFactory.class, env);
    }

    @Override
    public Storage newObject(final String type, final Config cfg) {
        final StorageFactory factory = super.factories.get(type);
        if (factory == null) {
            throw new StorageNotFoundException(type);
        }
        return factory.newStorage(cfg);
    }

    /**
     * Known storage types.
     *
     * @return Set of storage types.
     */
    public Set<String> types() {
        return this.factories.keySet();
    }

    @Override
    public Set<String> defPackages() {
        return Collections.singleton("com.artipie.asto");
    }

    @Override
    public String scanPackagesEnv() {
        return StoragesLoader.SCAN_PACK;
    }

    @Override
    public String getFactoryName(final Class<?> element) {
        return Arrays.stream(element.getAnnotations())
            .filter(ArtipieStorageFactory.class::isInstance)
            .map(a -> ((ArtipieStorageFactory) a).value())
            .findFirst()
            .orElseThrow(
                () -> new ArtipieException("Annotation 'ArtipieStorageFactory' should have a not empty value")
            );
    }
}
