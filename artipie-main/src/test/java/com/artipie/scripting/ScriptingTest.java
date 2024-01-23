/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.scripting;

import java.util.HashMap;
import java.util.Map;
import javax.script.ScriptException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.Test;

/**
 * Tests Script.StandardScript and Script.PrecompiledScript.
 * @since 0.1
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class ScriptingTest {

    /**
     * Source code for scripts testing. Currently identical for all languages.
     */
    private static String srccode = "a = a * 3;\nb = a + 1;";

    @Test
    public void groovyTest() throws ScriptException {
        this.testScript(Script.ScriptType.GROOVY, ScriptingTest.srccode);
    }

    @Test
    public void mvelTest() throws ScriptException {
        this.testScript(Script.ScriptType.MVEL, ScriptingTest.srccode);
    }

    @Test
    public void pythonTest() throws ScriptException {
        this.testScript(Script.ScriptType.PYTHON, ScriptingTest.srccode);
    }

    @Test
    public void rubyTest() throws ScriptException {
        this.testScript(Script.ScriptType.RUBY, ScriptingTest.srccode);
    }

    @Test
    public void evalValueTest() throws ScriptException {
        final Script.Result result =
            new Script.PrecompiledScript(Script.ScriptType.GROOVY, "2 + 3").call();
        MatcherAssert.assertThat(this.toLong(result.value()), new IsEqual<>(5L));
    }

    private Long toLong(final Object value) {
        final Long result;
        if (value instanceof Long) {
            result = (Long) value;
        } else if (value instanceof Integer) {
            result = ((Integer) value).longValue();
        } else {
            throw new IllegalArgumentException();
        }
        return result;
    }

    private void testScript(
        final Script.ScriptType type, final String code
    ) throws ScriptException {
        final Script script = new Script.PrecompiledScript(type, code);
        final Map<String, Object> variables = new HashMap<>();
        variables.put("a", 4);
        final Script.Result res = script.call(variables);
        MatcherAssert.assertThat(this.toLong(res.variable("a")), new IsEqual<>(12L));
        MatcherAssert.assertThat(this.toLong(res.variable("b")), new IsEqual<>(13L));
        MatcherAssert.assertThat(res.variable("not_found"), new IsEqual<>(null));
        MatcherAssert.assertThat(this.toLong(variables.get("a")), new IsEqual<>(12L));
        MatcherAssert.assertThat(this.toLong(variables.get("b")), new IsEqual<>(13L));
        MatcherAssert.assertThat(variables.get("not_found"), new IsEqual<>(null));
    }
}
