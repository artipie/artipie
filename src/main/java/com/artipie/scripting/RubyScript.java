/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.scripting;

/**
 * Ruby Script.
 *
 * @since 0.30
 */
public interface RubyScript {
    /**
     * Name of scripting engine.
     */
    String NAME = "ruby";

    /**
     * Create instance of {@link Script}.
     * @param script Script code
     * @return Script
     */
    @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
    static Script newScript(final String script) {
        return Script.newScript(RubyScript.NAME, script);
    }

    /**
     * Create precompiled instance of {@link Script}.
     * @param script Script code
     * @return Script
     */
    @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
    static Script newCompiledScript(final String script) {
        return Script.newCompiledScript(RubyScript.NAME, script);
    }
}
