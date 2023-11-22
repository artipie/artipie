/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.scripting;

import com.artipie.ArtipieException;
import com.artipie.asto.Key;
import com.jcabi.log.Logger;
import java.util.HashMap;
import java.util.Map;
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
            final Script.PrecompiledScript script = scontext.getScripts().getUnchecked(key);
            try {
                final Map<String, Object> vars = new HashMap<>();
                vars.put("_settings", scontext.getSettings());
                vars.put("_repositories", scontext.getRepositories());
                script.call(vars);
            } catch (final ScriptException exc) {
                Logger.error(
                    ScriptRunner.class,
                    "Execution error in script %s %[exception]s",
                    key.toString(),
                    exc
                );
            }
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
}
