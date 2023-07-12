/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.scripting;

import com.artipie.ArtipieException;
import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.settings.Settings;
import com.artipie.settings.repo.RepositoriesFromStorage;
import com.jcabi.log.Logger;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
 */
public final class ScriptRunner implements Job {
    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        final Settings settings = (Settings) context.getJobDetail().getJobDataMap().get("settings");
        final Key key = new Key.From(context.getJobDetail().getJobDataMap().getString("key"));
        final BlockingStorage storage = new BlockingStorage(settings.configStorage());
        if (settings == null || key == null || storage == null) {
            this.stopJob(context);
            return;
        }
        if (storage.exists(key)) {
            extension(key.toString())
                .flatMap(ext -> script(ext, new String(storage.value(key))))
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
     * Create instance of Script by script-file extension and script code.
     * @param ext Extension of script-file.
     * @param script Script code.
     * @return Script instance
     */
    private static Optional<Script> script(final String ext, final String script) {
        final Map<String, Script.ScriptType> map = Map.of(
            "groovy", Script.ScriptType.GROOVY,
            "py", Script.ScriptType.PYTHON,
            "ruby", Script.ScriptType.RUBY,
            "mvel", Script.ScriptType.MVEL
        );
        Optional<Script> result = Optional.empty();
        final Script.ScriptType type = map.getOrDefault(ext, Script.ScriptType.NONE);
        if (!type.equals(Script.ScriptType.NONE)) {
            result = Optional.of(new Script.StandardScript(type, script));
        }
        return result;
    }

    /**
     * Obtain extension of filename.
     * @param filename Name of file.
     * @return Extension.
     */
    private static Optional<String> extension(final String filename) {
        final int pos = filename.lastIndexOf('.');
        final Optional<String> res;
        if (pos >= 0) {
            res = Optional.of(filename.substring(pos + 1));
        } else {
            res = Optional.empty();
        }
        return res;
    }
}
