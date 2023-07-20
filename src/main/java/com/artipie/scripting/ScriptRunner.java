/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.scripting;

import com.artipie.ArtipieException;
import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.misc.ArtipieProperties;
import com.artipie.misc.Property;
import com.artipie.settings.Settings;
import com.artipie.settings.repo.RepositoriesFromStorage;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.jcabi.log.Logger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.script.ScriptException;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

/**
 * Script runner.
 * Job for running script in quartz
 *
 * @since 0.30
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class ScriptRunner implements Job {

    /**
     * Cache for precompiled scripts.
     */
    private static LoadingCache<ScriptRunner.FilesContent, Script.PrecompiledScript> scripts;

    static {
        final long duration = new Property(ArtipieProperties.SCRIPTS_TIMEOUT)
            .asLongOrDefault(120_000L);
        ScriptRunner.scripts = CacheBuilder.newBuilder()
            .expireAfterWrite(duration, TimeUnit.MILLISECONDS)
            .softValues()
            .build(
                new CacheLoader<>() {
                    @Override
                    public Script.PrecompiledScript load(final ScriptRunner.FilesContent content) {
                        return content.precompiledScript();
                    }
                }
            );
    }

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        final Settings settings = (Settings) context.getJobDetail().getJobDataMap().get("settings");
        final String parts = context.getJobDetail().getJobDataMap().getString("key");
        if (settings == null || parts == null) {
            this.stopJob(context);
            return;
        }
        final Key key = new Key.From(parts);
        final BlockingStorage storage = new BlockingStorage(settings.configStorage());
        if (storage.exists(key)) {
            cachedScript(key, storage)
                .map(
                    script -> {
                        Optional<Script.Result> result;
                        try {
                            final Map<String, Object> vars = new HashMap<>();
                            vars.put("_settings", settings);
                            vars.put("_repositories", new RepositoriesFromStorage(settings));
                            result = Optional.of(script.call(vars));
                        } catch (final ScriptException exc) {
                            Logger.error(
                                ScriptRunner.class,
                                "Execution error in script %s %[exception]s",
                                key.toString(),
                                exc
                            );
                            result = Optional.empty();
                        }
                        return result;
                    }
                );
        } else {
            Logger.warn(ScriptRunner.class, "Cannot find script %s", key.toString());
        }
    }

    /**
     * Stops the job and logs error.
     * @param context Job context
     */
    private void stopJob(final JobExecutionContext context) {
        final JobKey key = context.getJobDetail().getKey();
        try {
            Logger.error(this, String.format("Force stopping job %s...", key));
            new StdSchedulerFactory().getScheduler().deleteJob(key);
            Logger.error(this, String.format("Job %s stopped.", key));
        } catch (final SchedulerException error) {
            Logger.error(this, String.format("Error while stopping job %s", key));
            throw new ArtipieException(error);
        }
    }

    /**
     * Returns cached script. Scripts objects are handled by LoadingCache.
     * @param key Key in the `storage` for the script.
     * @param storage Targer storage object.
     * @return Returns cached script or empty Optional in case of error.
     */
    private static Optional<Script> cachedScript(final Key key, final BlockingStorage storage) {
        final String ext = ScriptRunner.extension(key.string());
        final Script.ScriptType type = Arrays.stream(Script.ScriptType.values())
            .filter(val -> val.ext().equals(ext)).findFirst().orElse(Script.ScriptType.NONE);
        Optional<Script> result = Optional.empty();
        if (!type.equals(Script.ScriptType.NONE)) {
            final ScriptRunner.FilesContent content = new ScriptRunner.FilesContent(
                key, storage
            );
            result = Optional.ofNullable(ScriptRunner.scripts.getUnchecked(content));
        }
        return result;
    }

    /**
     * Obtain extension of filename.
     * @param filename Name of file.
     * @return Extension.
     */
    private static String extension(final String filename) {
        final int pos = filename.lastIndexOf('.');
        final String res;
        if (pos >= 0) {
            res = filename.substring(pos + 1);
        } else {
            res = "";
        }
        return res;
    }

    /**
     * Extra class for caching precompiled scripts.
     * @since 0.1
     */
    private static final class FilesContent {
        /**
         * Key.
         */
        private final Key key;

        /**
         * Storage.
         */
        private final BlockingStorage storage;

        /**
         * Ctor.
         * @param key Key
         * @param storage Storage
         */
        private FilesContent(final Key key, final BlockingStorage storage) {
            this.key = key;
            this.storage = storage;
        }

        @Override
        public int hashCode() {
            return this.key.hashCode() ^ this.storage.hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            final boolean res;
            if (obj == this) {
                res = true;
            } else if (obj instanceof ScriptRunner.FilesContent) {
                final ScriptRunner.FilesContent data = (ScriptRunner.FilesContent) obj;
                res = Objects.equals(this.key, data.key)
                    && Objects.equals(data.storage, this.storage);
            } else {
                res = false;
            }
            return res;
        }

        /**
         * Returns precompiled script for stored key & storage.
         * @return Returns precompiled script object instance.
         */
        public Script.PrecompiledScript precompiledScript() {
            final String ext = FilesContent.extension(this.key.string());
            final Script.ScriptType type = Arrays.stream(Script.ScriptType.values())
                .filter(val -> val.ext().equals(ext)).findFirst().orElse(Script.ScriptType.NONE);
            Script.PrecompiledScript result = null;
            if (!type.equals(Script.ScriptType.NONE)) {
                final String script = new String(this.storage.value(this.key));
                result = new Script.PrecompiledScript(type, script);
            }
            return result;
        }

        /**
         * Obtain extension of filename.
         * @param filename Name of file.
         * @return Extension.
         */
        private static String extension(final String filename) {
            final int pos = filename.lastIndexOf('.');
            final String res;
            if (pos >= 0) {
                res = filename.substring(pos + 1);
            } else {
                res = "";
            }
            return res;
        }

        /**
         * Create instance of Script by script-file extension and script code.
         * @param ext Extension of script-file.
         * @param script Script code.
         * @return Script instance
         */
        private static Optional<Script> script(final String ext, final String script) {
            final Script.ScriptType type = Arrays.stream(Script.ScriptType.values())
                .filter(val -> val.ext().equals(ext)).findFirst().orElse(Script.ScriptType.NONE);
            Optional<Script> result = Optional.empty();
            if (!type.equals(Script.ScriptType.NONE)) {
                result = Optional.of(new Script.PrecompiledScript(type, script));
            }
            return result;
        }
    }
}
