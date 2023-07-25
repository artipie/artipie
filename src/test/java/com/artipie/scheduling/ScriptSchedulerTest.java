/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.scheduling;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.artipie.asto.Key;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.scripting.ScriptRunner;
import com.artipie.settings.Settings;
import com.artipie.settings.YamlSettings;
import com.artipie.settings.repo.RepoConfig;
import com.artipie.settings.repo.RepoConfigYaml;
import com.artipie.settings.repo.Repositories;
import com.artipie.settings.repo.RepositoriesFromStorage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.awaitility.Awaitility;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.quartz.CronScheduleBuilder;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

/**
 * Test for ArtipieScheduler.
 * @since 0.30
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle VisibilityModifierCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class ScriptSchedulerTest {

    /**
     * Path to the test script file.
     */
    private static final String SCRIPT_PATH = "scripts/script.groovy";

    /**
     * Path to the test results file.
     */
    private static final String RESULTS_PATH = "scripts/result.txt";

    /**
     * Temp dir.
     */
    @TempDir
    Path temp;

    /**
     * Test data storage.
     */
    private BlockingStorage data;

    /**
     * Before each method creates test data storage instance.
     * @since 0.30
     */
    @BeforeEach
    void init() {
        this.data = new BlockingStorage(new FileStorage(this.temp));
        ScriptRunner.clearCache();
    }

    @Test
    void scheduleJob() throws SchedulerException {
        final AtomicReference<String> ref = new AtomicReference<>();
        final ScriptScheduler scheduler = new ScriptScheduler();
        scheduler.start();
        scheduler.scheduleJob("0/5 * * * * ?", TestJob.class, Map.of("ref", ref));
        Awaitility.waitAtMost(1, TimeUnit.MINUTES).until(() -> ref.get() != null);
        scheduler.stop();
        MatcherAssert.assertThat(
            ref.get(),
            new IsEqual<>("TestJob is done")
        );
    }

    @Test
    void runSimpleCronJob() throws IOException {
        final Key result = new Key.From(ScriptSchedulerTest.RESULTS_PATH);
        this.runCronScript(
            String.join(
                "\n",
                "File file = new File('%1$s')",
                "file.write 'Hello world'"
            )
        );
        MatcherAssert.assertThat(
            new String(this.data.value(result)),
            new IsEqual<>("Hello world")
        );
    }

    @Test
    void runCronJobWithSettingsObject() throws IOException {
        final Key result = new Key.From(ScriptSchedulerTest.RESULTS_PATH);
        final YamlSettings settings = this.runCronScript(
            String.join(
                "\n",
                "File file = new File('%1$s')",
                "mapping = _settings.crontab().get().values().iterator().next().asMapping()",
                "file.write mapping.string('key') + mapping.string('cronexp')"
            )
        );
        final YamlMapping mapping = settings.crontab().get().values().iterator().next().asMapping();
        final String key = mapping.string("key");
        final String cronexp = mapping.string("cronexp");
        MatcherAssert.assertThat(
            new String(this.data.value(result)),
            new IsEqual<>(String.join("", key, cronexp))
        );
    }

    @Test
    void runCronJobWithReposObject() throws Exception {
        final String repo = "my-repo";
        new RepoConfigYaml("maven")
            .withPath("/artipie/test/maven")
            .withUrl("http://test.url/artipie")
            .saveTo(new FileStorage(this.temp), repo);
        final Key result = new Key.From(ScriptSchedulerTest.RESULTS_PATH);
        final YamlSettings settings = this.runCronScript(
            String.join(
                "\n",
                "File file = new File('%1$s')",
                "cfg = _repositories.config('my-repo').toCompletableFuture().join()",
                "file.write cfg.toString()"
            )
        );
        final Repositories repos = new RepositoriesFromStorage(settings);
        final RepoConfig cfg = repos.config(repo).toCompletableFuture().join();
        MatcherAssert.assertThat(
            new String(this.data.value(result)),
            new IsEqual<>(cfg.toString())
        );
    }

    @Test
    void testScriptRunner() throws Exception {
        final Key key = new Key.From("");
        final String cronexp = "*/3 * * * * ?";
        final Settings settings = null;
        final StdSchedulerFactory factory = new StdSchedulerFactory();
        final Scheduler scheduler = factory.getScheduler();
        scheduler.start();
        final JobDataMap jobdata = new JobDataMap();
        jobdata.put("key", key);
        jobdata.put("settings", settings);
        final JobDetail job = JobBuilder
            .newJob()
            .ofType(ScriptRunner.class)
            .withIdentity(String.format("%s %s", cronexp, key))
            .setJobData(jobdata)
            .build();
        final Trigger trigger = TriggerBuilder.newTrigger()
            .withIdentity(
                String.format("trigger-%s", job.getKey()),
                "cron-group"
            )
            .withSchedule(CronScheduleBuilder.cronSchedule(cronexp))
            .forJob(job)
            .build();
        scheduler.scheduleJob(job, trigger);
        scheduler.shutdown(true);
    }

    private YamlSettings runCronScript(final String cronscript) throws IOException {
        final YamlSettings settings = new YamlSettings(
            Yaml.createYamlInput(
                String.join(
                    System.lineSeparator(),
                    "meta:",
                    "  storage:",
                    "    type: fs",
                    String.format("    path: %s", this.temp.toString()),
                    "  crontab:",
                    String.format("    - path: %s", ScriptSchedulerTest.SCRIPT_PATH),
                    "      cronexp: */3 * * * * ?"
                )
            )
            .readYamlMapping(),
            this.temp
        );
        final String filename = this.temp.resolve(ScriptSchedulerTest.RESULTS_PATH).toString();
        final String script = String.format(cronscript, filename.replace("\\", "\\\\"));
        this.data.save(new Key.From(ScriptSchedulerTest.SCRIPT_PATH), script.getBytes());
        final ScriptScheduler scheduler = new ScriptScheduler();
        scheduler.start();
        scheduler.loadCrontab(settings);
        final Key result = new Key.From(ScriptSchedulerTest.RESULTS_PATH);
        Awaitility.waitAtMost(1, TimeUnit.MINUTES).until(() -> this.data.exists(result));
        scheduler.stop();
        return settings;
    }

    /**
     * Job for scheduler.
     * @since 0.30
     */
    public static final class TestJob implements Job {
        @SuppressWarnings("unchecked")
        @Override
        public void execute(final JobExecutionContext context) throws JobExecutionException {
            final AtomicReference<String> ref = (AtomicReference<String>) context.getJobDetail()
                .getJobDataMap().get("ref");
            ref.set("TestJob is done");
        }
    }
}
