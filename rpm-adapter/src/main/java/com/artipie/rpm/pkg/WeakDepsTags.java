/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm.pkg;

import java.util.Locale;
import org.redline_rpm.header.AbstractHeader;

/**
 * Tags for package weak dependency.
 * <a href="https://rpm-software-management.github.io/rpm/manual/tags.html">Tags</a>.
 * <a href="https://rpm-software-management.github.io/rpm/manual/dependencies.html">Deps</a>.
 * @since 1.11
 * @checkstyle JavadocVariableCheck (500 lines)
 */
public enum WeakDepsTags implements AbstractHeader.Tag {

    RECOMMENDNAME(5046, STRING_ARRAY_ENTRY),
    RECOMMENDVERSION(5047, STRING_ARRAY_ENTRY),
    RECOMMENDFLAGS(5048, INT32_ENTRY),

    SUGGESTNAME(5049, STRING_ARRAY_ENTRY),
    SUGGESTVERSION(5050, STRING_ARRAY_ENTRY),
    SUGGESTFLAGS(5051, INT32_ENTRY),

    SUPPLEMENTNAME(5052, STRING_ARRAY_ENTRY),
    SUPPLEMENTVERSION(5053, STRING_ARRAY_ENTRY),
    SUPPLEMENTFLAGS(5054, INT32_ENTRY),

    ENHANCENAME(5055, STRING_ARRAY_ENTRY),
    ENHANCEVERSION(5056, STRING_ARRAY_ENTRY),
    ENHANCEFLAGS(5057, INT32_ENTRY);

    /**
     * Tag code.
     */
    private final int code;

    /**
     * Data type.
     */
    private final int type;

    /**
     * Ctor.
     * @param code Tag code
     * @param type Data type
     */
    WeakDepsTags(final int code, final int type) {
        this.code = code;
        this.type = type;
    }

    @Override
    public int getCode() {
        return this.code;
    }

    @Override
    public int getType() {
        return this.type;
    }

    @Override
    public String getName() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
