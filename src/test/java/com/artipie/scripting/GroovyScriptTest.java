/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.scripting;

import java.util.HashMap;
import java.util.Map;
import javax.script.ScriptException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GroovyScript}.
 *
 * @since 0.30
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class GroovyScriptTest {
    @Test
    public void standardScript() throws ScriptException {
        GroovyScript.newScript("println(1)").call();
        GroovyScript.newScript("println(a)").call(Map.of("a", 2));
        MatcherAssert.assertThat(
            GroovyScript.newScript("a * 2").call(Map.of("a", 3)).value(),
            new IsEqual<>(6)
        );
        final Map<String, Object> variables = new HashMap<>();
        variables.put("a", 4);
        MatcherAssert.assertThat(
            GroovyScript.newScript("a = a * 3").call(variables).variable("a"),
            new IsEqual<>(12)
        );
    }

    @Test
    public void precompiledScript() throws ScriptException {
        GroovyScript.newCompiledScript("println(1)").call();
        GroovyScript.newCompiledScript("println(a)").call(Map.of("a", 2));
        MatcherAssert.assertThat(
            GroovyScript.newCompiledScript("a * 2").call(Map.of("a", 3)).value(),
            new IsEqual<>(6)
        );
        final Map<String, Object> variables = new HashMap<>();
        variables.put("a", 4);
        MatcherAssert.assertThat(
            GroovyScript.newCompiledScript("a = a * 3").call(variables).variable("a"),
            new IsEqual<>(12)
        );
    }
}
