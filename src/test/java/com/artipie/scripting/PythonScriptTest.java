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
 * Tests for {@link PythonScript}.
 *
 * @since 0.30
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class PythonScriptTest {
    @Test
    public void standardScript() throws ScriptException {
        final Map<String, Object> variables = new HashMap<>();
        PythonScript.newScript("print(1)").call();
        variables.put("a", 2);
        PythonScript.newScript("print(a)").call(variables);
        variables.put("a", 3);
        MatcherAssert.assertThat(
            PythonScript.newScript("a * 2").call(variables).value(),
            new IsEqual<>(6)
        );
        variables.put("a", 4);
        MatcherAssert.assertThat(
            PythonScript.newScript("a = a * 3").call(variables).variable("a"),
            new IsEqual<>(12)
        );
    }

    @Test
    public void precompiledScript() throws ScriptException {
        final Map<String, Object> variables = new HashMap<>();
        PythonScript.newCompiledScript("print(1)").call();
        variables.put("a", 2);
        PythonScript.newCompiledScript("print(a)").call(variables);
        variables.put("a", 3);
        MatcherAssert.assertThat(
            PythonScript.newCompiledScript("a * 2").call(variables).value(),
            new IsEqual<>(6)
        );
        variables.put("a", 4);
        MatcherAssert.assertThat(
            PythonScript.newCompiledScript("a = a * 3").call(variables).variable("a"),
            new IsEqual<>(12)
        );
    }
}
