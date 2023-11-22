/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.security.perms;

import com.artipie.ArtipieException;
import com.artipie.asto.factory.FactoryLoader;
import java.security.PermissionCollection;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Load from the packages via reflection and instantiate permission factories object.
 * @since 1.2
 */
public final class PermissionsLoader extends
    FactoryLoader<PermissionFactory<PermissionCollection>, ArtipiePermissionFactory,
        PermissionConfig, PermissionCollection> {

    /**
     * Environment parameter to define packages to find permission factories.
     * Package names should be separated with semicolon ';'.
     */
    public static final String SCAN_PACK = "PERM_FACTORY_SCAN_PACKAGES";

    /**
     * Ctor to obtain factories according to env.
     */
    public PermissionsLoader() {
        this(System.getenv());
    }

    /**
     * Ctor.
     * @param env Environment
     */
    public PermissionsLoader(final Map<String, String> env) {
        super(ArtipiePermissionFactory.class, env);
    }

    @Override
    public Set<String> defPackages() {
        return Stream.of("com.artipie.security", "com.artipie.docker", "com.artipie.api.perms")
            .collect(Collectors.toSet());
    }

    @Override
    public String scanPackagesEnv() {
        return PermissionsLoader.SCAN_PACK;
    }

    @Override
    public PermissionCollection newObject(final String type, final PermissionConfig config) {
        final PermissionFactory<?> factory = this.factories.get(type);
        if (factory == null) {
            throw new ArtipieException(String.format("Permission type %s is not found", type));
        }
        return factory.newPermissions(config);
    }

    @Override
    public String getFactoryName(final Class<?> clazz) {
        return Arrays.stream(clazz.getAnnotations())
            .filter(ArtipiePermissionFactory.class::isInstance)
            .map(inst -> ((ArtipiePermissionFactory) inst).value())
            .findFirst()
            .orElseThrow(
                // @checkstyle LineLengthCheck (1 lines)
                () -> new ArtipieException("Annotation 'ArtipiePermissionFactory' should have a not empty value")
            );
    }
}
