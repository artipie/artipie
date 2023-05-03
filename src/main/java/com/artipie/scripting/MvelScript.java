/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.scripting;

/**
 * Groovy Script.
 *
 * @since 0.30
 */
public interface MvelScript {
    /**
     * Name of scripting engine.
     */
    String NAME = "mvel";

    /**
     * Create instance of {@link Script}.
     * @param script Script code
     * @return Script
     */
    @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
    static Script newScript(final String script) {
        return Script.newScript(MvelScript.NAME, script);
    }

    /**
     * Create precompiled instance of {@link Script}.
     * @param script Script code
     * @return Script
     */
    @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
    static Script newCompiledScript(final String script) {
        return Script.newCompiledScript(MvelScript.NAME, script);
    }
}
