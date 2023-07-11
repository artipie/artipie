/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.scripting;

import com.artipie.scripting.Script.PrecompiledScript;
import com.artipie.scripting.Script.ScriptType;
import com.artipie.scripting.Script.StandardScript;
import java.util.HashMap;
import java.util.Map;
import javax.script.ScriptException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests for Python support by {@link Script}.
 *
 * @since 0.30
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class PythonScriptTest {
    @Test
    public void standardScript() throws ScriptException {
        final Map<String, Object> variables = new HashMap<>();
        new StandardScript(ScriptType.PYTHON, "print(1)").call();
        variables.put("a", 2);
        new StandardScript(ScriptType.PYTHON, "print(a)").call(variables);
        variables.put("a", 3);
        MatcherAssert.assertThat(
            new StandardScript(ScriptType.PYTHON, "a * 2").call(variables).value(),
            new IsEqual<>(6)
        );
        variables.put("a", 4);
        MatcherAssert.assertThat(
            new StandardScript(ScriptType.PYTHON, "a = a * 3").call(variables).variable("a"),
            new IsEqual<>(12)
        );
    }

    @Test
    public void precompiledScript() throws ScriptException {
        final Map<String, Object> variables = new HashMap<>();
        new PrecompiledScript(ScriptType.PYTHON, "print(1)").call();
        variables.put("a", 2);
        new PrecompiledScript(ScriptType.PYTHON, "print(a)").call(variables);
        variables.put("a", 3);
        MatcherAssert.assertThat(
            new PrecompiledScript(ScriptType.PYTHON, "a * 2").call(variables).value(),
            new IsEqual<>(6)
        );
        variables.put("a", 4);
        MatcherAssert.assertThat(
            new PrecompiledScript(ScriptType.PYTHON, "a = a * 3").call(variables).variable("a"),
            new IsEqual<>(12)
        );
    }
}
