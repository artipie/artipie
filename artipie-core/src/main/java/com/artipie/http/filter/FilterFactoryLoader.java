/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.http.filter;

import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.ArtipieException;
import com.artipie.asto.factory.FactoryLoader;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Load annotated by {@link ArtipieFilterFactory} annotation {@link FilterFactory} classes
 * from the packages via reflection and instantiate filters.
 * @since 1.2
 */
public final class FilterFactoryLoader extends
    FactoryLoader<FilterFactory, ArtipieFilterFactory,
    YamlMapping, Filter> {

    /**
     * Environment parameter to define packages to find filter factories.
     * Package names should be separated with semicolon ';'.
     */
    public static final String SCAN_PACK = "FILTER_FACTORY_SCAN_PACKAGES";

    /**
     * Ctor to obtain factories according to env.
     */
    public FilterFactoryLoader() {
        this(System.getenv());
    }

    /**
     * Ctor.
     * @param env Environment
     */
    public FilterFactoryLoader(final Map<String, String> env) {
        super(ArtipieFilterFactory.class, env);
    }

    @Override
    public Set<String> defPackages() {
        return Stream.of("com.artipie.http.filter").collect(Collectors.toSet());
    }

    @Override
    public String scanPackagesEnv() {
        return FilterFactoryLoader.SCAN_PACK;
    }

    @Override
    public Filter newObject(final String type, final YamlMapping yaml) {
        final FilterFactory factory = this.factories.get(type);
        if (factory == null) {
            throw new ArtipieException(
                String.format(
                    "%s type %s is not found",
                    Filter.class.getSimpleName(),
                    type
                )
            );
        }
        return factory.newFilter(yaml);
    }

    @Override
    public String getFactoryName(final Class<?> clazz) {
        return Arrays.stream(clazz.getAnnotations())
            .filter(ArtipieFilterFactory.class::isInstance)
            .map(inst -> ((ArtipieFilterFactory) inst).value())
            .findFirst()
            .orElseThrow(
                // @checkstyle LineLengthCheck (1 lines)
                () -> new ArtipieException(
                    String.format(
                        "Annotation '%s' should have a not empty value",
                        ArtipieFilterFactory.class.getSimpleName()
                    )
                )
            );
    }
}
