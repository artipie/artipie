/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.scheduling;

import com.artipie.ArtipieException;
import com.artipie.maven.MavenProxyPackageProcessor;
import com.artipie.npm.events.NpmProxyPackageProcessor;
import com.artipie.pypi.PyProxyPackageProcessor;
import com.artipie.settings.repo.RepoConfig;
import com.jcabi.log.Logger;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.SchedulerException;

/**
 * Artifacts metadata events queues.
 * <p>
 * 1) This class holds events queue {@link MetadataEventQueues#eventQueue()} for all the adapters,
 * this queue is passed to adapters, adapters adds packages metadata on upload/delete to the queue.
 * Queue is periodically processed by {@link com.artipie.scheduling.EventsProcessor} and consumed
 * by {@link com.artipie.db.DbConsumer}.
 * <p>
 * 2) This class also holds queues for proxy adapters (maven, npm, pypi). Each proxy repository
 * has its own queue with packages metadata ({@link MetadataEventQueues#queues}) and its own quartz
 * job to process this queue. The queue and job for concrete proxy repository are created/started
 * on the first queue request. If proxy repository is removed, jobs are stopped
 * and queue is removed.
 * @since 0.31
 */
public final class MetadataEventQueues {

    /**
     * Name of the yaml proxy repository settings and item in job data map for npm-proxy.
     */
    private static final String HOST = "host";

    /**
     * Map with proxy adapters name and queue.
     */
    private final Map<String, Queue<ProxyArtifactEvent>> queues;

    /**
     * Map with proxy adapters name and corresponding quartz jobs keys.
     */
    private final Map<String, Set<JobKey>> keys;

    /**
     * Artifact events queue.
     */
    private final Queue<ArtifactEvent> queue;

    /**
     * Quartz service.
     */
    private final QuartzService quartz;

    /**
     * Ctor.
     *
     * @param queue Artifact events queue
     * @param quartz Quartz service
     */
    public MetadataEventQueues(
        final Queue<ArtifactEvent> queue, final QuartzService quartz
    ) {
        this.queue = queue;
        this.queues = new ConcurrentHashMap<>();
        this.quartz = quartz;
        this.keys = new ConcurrentHashMap<>();
    }

    /**
     * Artifact events queue.
     * @return Artifact events queue
     */
    public Queue<ArtifactEvent> eventQueue() {
        return this.queue;
    }

    /**
     * Obtain queue for proxy adapter repository.
     * @param config Repository config
     * @return Queue for proxy events
     * @checkstyle ExecutableStatementCountCheck (30 lines)
     */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public Optional<Queue<ProxyArtifactEvent>> proxyEventQueues(final RepoConfig config) {
        Optional<Queue<ProxyArtifactEvent>> result =
            Optional.ofNullable(this.queues.get(config.name()));
        if (result.isEmpty() && config.storageOpt().isPresent()) {
            try {
                final Queue<ProxyArtifactEvent> events = this.queues.computeIfAbsent(
                    config.name(),
                    key -> {
                        final Queue<ProxyArtifactEvent> res = new ConcurrentLinkedQueue<>();
                        final JobDataMap data = new JobDataMap();
                        data.put("packages", res);
                        data.put("storage", config.storage());
                        data.put("events", this.queue);
                        final ProxyRepoType type = ProxyRepoType.type(config.type());
                        if (type == ProxyRepoType.NPM_PROXY) {
                            data.put(MetadataEventQueues.HOST, artipieHost(config));
                        }
                        final int threads = Math.max(1, settingsIntValue(config, "threads_count"));
                        final int interval = Math.max(
                            1, settingsIntValue(config, "interval_seconds")
                        );
                        try {
                            this.keys.put(
                                config.name(),
                                this.quartz.schedulePeriodicJob(interval, threads, type.job(), data)
                            );
                            // @checkstyle LineLengthCheck (1 line)
                            Logger.info(this, "Initialized proxy metadata job and queue for %s repository", config.name());
                        } catch (final SchedulerException err) {
                            throw new ArtipieException(err);
                        }
                        return res;
                    }
                );
                result = Optional.of(events);
            // @checkstyle IllegalCatchCheck (5 lines)
            } catch (final Exception err) {
                Logger.error(
                    this, "Failed to initialize events queue processing for repo %s:\n%s",
                    config.name(), err.getMessage()
                );
                result = Optional.empty();
            }
        }
        return result;
    }

    /**
     * Stops proxy repository events processing and removes corresponding queue.
     * @param name Repository name
     */
    public void stopProxyMetadataProcessing(final String name) {
        final Set<JobKey> set = this.keys.remove(name);
        if (set != null) {
            set.forEach(this.quartz::deleteJob);
        }
        this.queues.remove(name);
    }

    /**
     * Get integer value from settings.
     * @param config Repo config
     * @param key Setting name key
     * @return Int value from repository setting section, -1 if not present
     */
    private static int settingsIntValue(final RepoConfig config, final String key) {
        return config.settings().map(yaml -> yaml.integer(key)).orElse(-1);
    }

    /**
     * Artipie server external host. Required for npm proxy adapter only.
     * @param config Repository config
     * @return The host
     */
    private static String artipieHost(final RepoConfig config) {
        return config.settings()
            .flatMap(yaml -> Optional.ofNullable(yaml.string(MetadataEventQueues.HOST)))
            .orElse("unknown");
    }

    /**
     * Repository types.
     * @since 0.31
     * @checkstyle JavadocVariableCheck (30 lines)
     */
    enum ProxyRepoType {

        MAVEN_PROXY {
            @Override
            Class<? extends QuartzJob> job() {
                return MavenProxyPackageProcessor.class;
            }
        },

        PYPI_PROXY {
            @Override
            Class<? extends QuartzJob> job() {
                return PyProxyPackageProcessor.class;
            }
        },

        NPM_PROXY {
            @Override
            Class<? extends QuartzJob> job() {
                return NpmProxyPackageProcessor.class;
            }
        };

        /**
         * Class of the corresponding quartz job.
         * @return Class of the quartz job
         */
        abstract Class<? extends QuartzJob> job();

        /**
         * Get enum item by string repo type.
         * @param val String repo type
         * @return Item enum value
         */
        static ProxyRepoType type(final String val) {
            return ProxyRepoType.valueOf(val.toUpperCase(Locale.ROOT).replace("-", "_"));
        }
    }

}
