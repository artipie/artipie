/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.asto.factory;

import com.artipie.ArtipieException;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.jcabi.log.Logger;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

/**
 * Loader for various factories for different objects.
 * @param <F> Factory class
 * @param <A> Factory annotation class
 * @param <C> Config class
 * @param <O> Object to instantiate class
 * @since 1.16
 */
@SuppressWarnings({"this-escape", "unchecked"})
public abstract class FactoryLoader<F, A, C, O> {

    /**
     * The name of the factory <-> factory.
         */
    protected final Map<String, F> factories;

    /**
     * Annotation class.
     */
    private final Class<A> annot;

    /**
     * Ctor.
     * @param annot Annotation class
     * @param env Environment
     */
    protected FactoryLoader(final Class<A> annot, final Map<String, String> env) {
        this.annot = annot;
        this.factories = this.init(env);
    }

    /**
     * Default packages names.
     * @return The names of the default scan package
     */
    public abstract Set<String> defPackages();

    /**
     * Environment parameter to define packages to find factories.
     * Package names should be separated by semicolon ';'.
     * @return Env param name
     */
    public abstract String scanPackagesEnv();

    /**
     * Find factory by name and create object.
     * @param name The factory name
     * @param config Configuration
     * @return The object
     */
    public abstract O newObject(String name, C config);

    /**
     * Get the name of the factory from provided element. Call {@link Class#getAnnotations()}
     * method on the element, filter required annotations and get factory implementation name.
     * @param element Element to get annotations from
     * @return The name of the factory
     */
    public abstract String getFactoryName(Class<?> element);

    /**
     * Finds and initiates annotated classes in default and env packages.
     *
     * @param env Environment parameters.
     * @return Map of StorageFactories.
     */
    private Map<String, F> init(final Map<String, String> env) {
        final List<String> pkgs = Lists.newArrayList(this.defPackages());
        final String pgs = env.get(this.scanPackagesEnv());
        if (!Strings.isNullOrEmpty(pgs)) {
            pkgs.addAll(Arrays.asList(pgs.split(";")));
        }
        final Map<String, F> res = new HashMap<>();
        pkgs.forEach(
            pkg -> new Reflections(pkg)
                .get(Scanners.TypesAnnotated.with(this.annot).asClass())
                .forEach(
                    element -> {
                        final String type = this.getFactoryName(element);
                        final F existed = res.get(type);
                        if (existed != null) {
                            throw new ArtipieException(
                                String.format(
                                    "Factory with type '%s' already exists [class=%s].",
                                    type, existed.getClass().getSimpleName()
                                )
                            );
                        }
                        try {
                            res.put(type, (F) element.getDeclaredConstructor().newInstance());
                            Logger.info(
                                StoragesLoader.class,
                                "Initiated factory [type=%s, class=%s]",
                                type, element.getSimpleName()
                            );
                        } catch (final InstantiationException | IllegalAccessException
                            | InvocationTargetException | NoSuchMethodException err) {
                            throw new ArtipieException(err);
                        }
                    }
                )
        );
        return res;
    }

}
