/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.scripting;

import com.artipie.ArtipieException;
import java.util.Map;
import java.util.Objects;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

/**
 * Script.
 *
 * @since 0.30
 */
public interface Script {
    /**
     * Script engine manager.
     */
    ScriptEngineManager MANAGER = new ScriptEngineManager();

    /**
     * Call script.
     * @return Result of script execution.
     * @throws ScriptException Script execution exception
     */
    Result call() throws ScriptException;

    /**
     * Call script by passing environment variables.
     * @param vars Environment variables
     * @return Result of script execution.
     * @throws ScriptException Script execution exception
     */
    Result call(Map<String, Object> vars) throws ScriptException;

    /**
     * Create instance of {@link Script}.
     * @param name Name of scripting engine.
     * @param script Script code
     * @return Script
     */
    @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
    static Script newScript(final String name, final String script) {
        final ScriptEngine engine = Script.MANAGER.getEngineByName(name);
        return new StandardScript(engine, script);
    }

    /**
     * Create precompiled instance of {@link Script}.
     * @param name Name of scripting engine.
     * @param script Script code
     * @return Script
     */
    @SuppressWarnings("PMD.ProhibitPublicStaticMethods")
    static Script newCompiledScript(final String name, final String script) {
        final ScriptEngine engine = Script.MANAGER.getEngineByName(name);
        return new PrecompiledScript(engine, script);
    }

    /**
     * Standard implementation of {@link Script}.
     * @since 0.30
     */
    class StandardScript implements Script {
        /**
         * Scripting engine.
         */
        private final ScriptEngine engine;

        /**
         * Script code.
         */
        private final String script;

        /**
         * Ctor.
         * @param engine Scripting engine
         * @param script Script code
         */
        public StandardScript(final ScriptEngine engine, final String script) {
            this.engine = engine;
            this.script = Objects.requireNonNull(script, "Script is null");
        }

        @Override
        public Result call() throws ScriptException {
            final Result result = new Result();
            result.value(this.engine.eval(this.script, result.context()));
            return result;
        }

        @Override
        public Result call(final Map<String, Object> vars) throws ScriptException {
            final Result result = new Result(vars);
            result.value(this.engine.eval(this.script, result.context()));
            return result;
        }
    }

    /**
     * Precompiled implementation of {@link Script}.
     * Should be used for multiple invocations of script.
     * @since 0.30
     */
    @SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
    class PrecompiledScript implements Script {
        /**
         * Compiled script.
         */
        private final CompiledScript script;

        /**
         * Ctor.
         * @param engine Scripting engine
         * @param script Script code
         */
        public PrecompiledScript(final ScriptEngine engine, final String script) {
            if (!(engine instanceof Compilable)) {
                throw new ArtipieException(
                    String.format("Scripting engine '%s' does not support compilation", engine)
                );
            }
            try {
                this.script = ((Compilable) engine).compile(script);
            } catch (final ScriptException exc) {
                throw new ArtipieException(exc);
            }
        }

        @Override
        public Result call() throws ScriptException {
            final Result result = new Result();
            result.value(this.script.eval(result.context()));
            return result;
        }

        @Override
        public Result call(final Map<String, Object> vars) throws ScriptException {
            final Result result = new Result(vars);
            result.value(this.script.eval(result.context()));
            return result;
        }
    }

    /**
     * Result of script invocation.
     * @since 0.30
     */
    @SuppressWarnings(
        {
            "PMD.OnlyOneConstructorShouldDoInitialization",
            "PMD.ConstructorOnlyInitializesOrCallOtherConstructors"
        }
    )
    class Result {
        /**
         * Script context.
         */
        private final ScriptContext context;

        /**
         * Resulting value.
         */
        private Object val;

        /**
         * Ctor.
         */
        public Result() {
            this.context = new SimpleScriptContext();
        }

        /**
         * Ctor.
         * @param vars Environment variables
         */
        public Result(final Map<String, Object> vars) {
            this();
            this.context.setBindings(new SimpleBindings(vars), ScriptContext.ENGINE_SCOPE);
        }

        /**
         * Resulting value.
         * @return Value
         */
        public Object value() {
            return this.val;
        }

        /**
         * Environment variable by name.
         * @param name Environment variable
         * @return Environment variable
         */
        public Object variable(final String name) {
            return this.context.getBindings(ScriptContext.ENGINE_SCOPE).get(name);
        }

        /**
         * Script context.
         * @return ScriptContext
         */
        private ScriptContext context() {
            return this.context;
        }

        /**
         * Setter for resulting value.
         * @param value Resulting value
         */
        private void value(final Object value) {
            this.val = value;
        }
    }
}
