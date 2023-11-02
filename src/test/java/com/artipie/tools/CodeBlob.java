/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.tools;

import java.util.Objects;

/**
 * Class stores classname and it's compiled byte code.
 * @since 0.28.0
 */
public final class CodeBlob {
    /**
     * Class name of class.
     * It is used by class loader as classname.
     */
    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    private final String classname;

    /**
     * Byte code of class.
     */
    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    private final byte[] blob;

    /**
     * Ctor.
     * @param classname Class name of class.
     * @param bytes Byte code of class
     */
    @SuppressWarnings("PMD.ArrayIsStoredDirectly")
    public CodeBlob(final String classname, final byte[] bytes) {
        this.classname = classname;
        this.blob = bytes;
    }

    /**
     * Class name of class.
     * @return Class name.
     */
    public String classname() {
        return this.classname;
    }

    /**
     * Byte code of class.
     * @return Byte code.
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public byte[] blob() {
        return this.blob;
    }

    @SuppressWarnings("PMD.OnlyOneReturn")
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final CodeBlob other = (CodeBlob) obj;
        return Objects.equals(this.classname, other.classname);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.classname);
    }
}
