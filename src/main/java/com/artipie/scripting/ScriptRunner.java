/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.scripting;

import com.artipie.ArtipieException;
import com.artipie.asto.Key;
import com.jcabi.log.Logger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.script.ScriptException;
import org.apache.commons.io.FilenameUtils;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

/**
 * Script runner.
 * Job for running script in quartz
 * @since 0.30
 */
public final class ScriptRunner implements Job {

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        final ScriptContext scontext = (ScriptContext) context
            .getJobDetail().getJobDataMap().get("context");
        final Key key = (Key) context.getJobDetail().getJobDataMap().get("key");
        if (scontext == null || key == null) {
            this.stopJob(context);
            return;
        }
        if (scontext.getStorage().exists(key)) {
            ScriptRunner.cachedScript(key, scontext)
                .map(
                    script -> {
                        Optional<Script.Result> result;
                        try {
                            final Map<String, Object> vars = new HashMap<>();
                            vars.put("_settings", scontext.getSettings());
                            vars.put("_repositories", scontext.getRepositories());
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
     * @param key Key in the `scontext` storage for the script.
     * @param scontext Target scontext object.
     * @return Returns cached script or empty Optional in case of error.
     */
    private static Optional<Script> cachedScript(final Key key, final ScriptContext scontext) {
        final String ext = FilenameUtils.getExtension(key.string());
        final Script.ScriptType type = Arrays.stream(Script.ScriptType.values())
            .filter(val -> val.ext().equals(ext)).findFirst().orElse(Script.ScriptType.NONE);
        Optional<Script> result = Optional.empty();
        if (!type.equals(Script.ScriptType.NONE)) {
            final ScriptContext.FilesContent content = new ScriptContext.FilesContent(
                key, scontext.getStorage()
            );
            result = Optional.ofNullable(scontext.getScripts().getUnchecked(content));
        }
        return result;
    }
}
