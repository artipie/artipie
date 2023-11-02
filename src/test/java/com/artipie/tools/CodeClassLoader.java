/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.tools;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Classloader of dynamically compiled classes.
 * @since 0.28
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.ConstructorShouldDoInitialization")
public final class CodeClassLoader extends ClassLoader {
    /**
     * Code blobs.
     */
    private final Map<String, CodeBlob> blobs = new TreeMap<>();

    /**
     * Ctor.
     */
    public CodeClassLoader() {
        super();
    }

    /**
     * Ctor.
     * @param parent Parent class loader.
     */
    public CodeClassLoader(final ClassLoader parent) {
        super(parent);
    }

    /**
     * Adds code blobs.
     * @param blobs Code blobs.
     * @checkstyle HiddenFieldCheck (5 lines)
     */
    public void addBlobs(final CodeBlob... blobs) {
        this.addBlobs(List.of(blobs));
    }

    /**
     * Adds code blobs.
     * @param blobs Code blobs.
     * @checkstyle HiddenFieldCheck (5 lines)
     */
    public void addBlobs(final List<CodeBlob> blobs) {
        blobs.forEach(blob -> this.blobs.put(blob.classname(), blob));
    }

    @Override
    public Class<?> findClass(final String name) throws ClassNotFoundException {
        final Class<?> clazz;
        if (this.blobs.containsKey(name)) {
            final byte[] code = this.blobs.get(name).blob();
            clazz = defineClass(name, code, 0, code.length);
        } else {
            clazz = super.findClass(name);
        }
        return clazz;
    }
}
