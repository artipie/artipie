/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.scripting;

import com.artipie.ArtipieException;
import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import java.util.Arrays;
import java.util.Map;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;
import org.apache.commons.io.FilenameUtils;

/**
 * Script.
 *
 * @since 0.30
 */
public interface Script {

    /**
     * Script type values, known by javax.script.ScriptEngineManager.
     */
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    enum ScriptType {

        /**
         * No valid script type was provided.
         */
        NONE("", ""),

        /**
         * Groovy script type.
         */
        GROOVY("groovy", "groovy"),

        /**
         * Mvel script type.
         */
        MVEL("mvel", "mvel"),

        /**
         * PPython script type.
         */
        PYTHON("python", "py"),

        /**
         * Ruby script type.
         */
        RUBY("ruby", "rb");

        /**
         * Script language name, for ScriptEngineManager.
         */
        private final String value;

        /**
         * Corresponding file extension.
         */
        private final String ext;

        /**
         * Script type value ctor.
         * @param value Script language name.
         * @param ext Corresponding file extension.
         */
        ScriptType(final String value, final String ext) {
            this.value = value;
            this.ext = ext;
        }

        @Override
        public String toString() {
            return this.value;
        }

        /**
         * Returns corresponding file extension.
         * @return File extension as String.
         */
        public String ext() {
            return this.ext;
        }
    }

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
         * @param key Key for the script in the storage.
         * @param storage Target storage.
         */
        public PrecompiledScript(final Key key, final BlockingStorage storage) {
            this(PrecompiledScript.getScriptType(key), new String(storage.value(key)));
        }

        /**
         * Ctor.
         * @param type Name of scripting engine.
         * @param script Script code
         */
        public PrecompiledScript(final ScriptType type, final String script) {
            final ScriptEngine engine = Script.MANAGER.getEngineByName(type.toString());
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
            result.setValue(this.script.eval(result.context()));
            return result;
        }

        @Override
        public Result call(final Map<String, Object> vars) throws ScriptException {
            final Result result = new Result(vars);
            result.setValue(this.script.eval(result.context()));
            return result;
        }

        /**
         * Provides script type based on the storage key of the script.
         * @param key Storage Key of the script.
         * @return ScriptType on success.
         * @throws ArtipieException in case of the unknown script type.
         */
        private static ScriptType getScriptType(final Key key) {
            final String ext = FilenameUtils.getExtension(key.string());
            final ScriptType type = Arrays.stream(ScriptType.values())
                .filter(val -> val.ext().equals(ext)).findFirst().orElse(ScriptType.NONE);
            if (type.equals(ScriptType.NONE)) {
                throw new ArtipieException(
                    String.join("", "Unknown script type for key: ", key.string())
                );
            }
            return type;
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
         * Resulting value on script evaluation (like eval()).
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
         * Resulting value on script evaluation.
         * @return Value
         */
        public Object value() {
            return this.val;
        }

        /**
         * Environment variable by name.
         * @param name Environment variable
         * @return Environment variable or null, if not found.
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
        private void setValue(final Object value) {
            this.val = value;
        }
    }
}
