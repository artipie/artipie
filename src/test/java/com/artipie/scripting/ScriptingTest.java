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
import org.junit.Test;

/**
 * Tests Script.StandardScript and Script.PrecompiledScript.
 * @since 0.1
 * @checkstyle MagicNumberCheck (200 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class ScriptingTest {

    @Test
    public void groovyStdTest() throws ScriptException {
        final String code = "a = a * 3\nb = a + 1";
        this.testScript(Script.ScriptType.GROOVY, code);
    }

    @Test
    public void groovyPrecompTest() throws ScriptException {
        final String code = "a = a * 3\nb = a + 1";
        this.testScript(Script.ScriptType.GROOVY, code);
    }

    @Test
    public void mvelStdTest() throws ScriptException {
        final String code = "a = a * 3;\nb = a + 1";
        this.testScript(Script.ScriptType.MVEL, code);
    }

    @Test
    public void mvelPrecompTest() throws ScriptException {
        final String code = "a = a * 3;\nb = a + 1";
        this.testScript(Script.ScriptType.MVEL, code);
    }

    @Test
    public void pythonStdTest() throws ScriptException {
        final String code = "a = a * 3\nb = a + 1";
        this.testScript(Script.ScriptType.PYTHON, code);
    }

    @Test
    public void pythonPrecompTest() throws ScriptException {
        final String code = "a = a * 3\nb = a + 1";
        this.testScript(Script.ScriptType.PYTHON, code);
    }

    @Test
    public void rubyStdTest() throws ScriptException {
        final String code = "a = a * 3;\nb = a + 1;";
        this.testScript(Script.ScriptType.RUBY, code);
    }

    @Test
    public void rubyPrecompTest() throws ScriptException {
        final String code = "a = a * 3;\nb = a + 1;";
        this.testScript(Script.ScriptType.RUBY, code);
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
