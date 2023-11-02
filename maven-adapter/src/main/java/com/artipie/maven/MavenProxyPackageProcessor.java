/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.maven;

import com.artipie.asto.Key;
import com.artipie.asto.Meta;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.KeyLastPart;
import com.artipie.maven.http.MavenSlice;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.scheduling.ProxyArtifactEvent;
import com.artipie.scheduling.QuartzJob;
import com.jcabi.log.Logger;
import java.util.Collection;
import java.util.Queue;
import org.quartz.JobExecutionContext;

/**
 * Processes artifacts uploaded by proxy and adds info to artifacts metadata events queue.
 * @since 0.10
 */
public final class MavenProxyPackageProcessor extends QuartzJob {

    /**
     * Repository type.
     */
    private static final String REPO_TYPE = "maven-proxy";

    /**
     * Artifact events queue.
     */
    private Queue<ArtifactEvent> events;

    /**
     * Queue with packages and owner names.
     */
    private Queue<ProxyArtifactEvent> packages;

    /**
     * Repository storage.
     */
    private Storage asto;

    @Override
    @SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.EmptyWhileStmt"})
    public void execute(final JobExecutionContext context) {
        if (this.asto == null || this.packages == null || this.events == null) {
            super.stopJob(context);
        } else {
            while (!this.packages.isEmpty()) {
                final ProxyArtifactEvent event = this.packages.poll();
                if (event != null) {
                    final Collection<Key> keys = this.asto.list(event.artifactKey()).join();
                    try {
                        final Key archive = MavenSlice.EVENT_INFO.artifactPackage(keys);
                        this.events.add(
                            new ArtifactEvent(
                                MavenProxyPackageProcessor.REPO_TYPE, event.repoName(), "ANONYMOUS",
                                MavenSlice.EVENT_INFO.formatArtifactName(
                                    event.artifactKey().parent().get()
                                ),
                                new KeyLastPart(event.artifactKey()).get(),
                                this.asto.metadata(archive)
                                    .thenApply(meta -> meta.read(Meta.OP_SIZE)).join().get()
                            )
                        );
                        // @checkstyle EmptyBlockCheck (1 line)
                        while (this.packages.remove(event)) { }
                    // @checkstyle IllegalCatchCheck (1 line)
                    } catch (final Exception err) {
                        Logger.error(
                            this,
                            String.format(
                                "Failed to process maven proxy package %s", event.artifactKey()
                            )
                        );
                    }
                }
            }
        }
    }

    /**
     * Setter for events queue.
     * @param queue Events queue
     */
    public void setEvents(final Queue<ArtifactEvent> queue) {
        this.events = queue;
    }

    /**
     * Packages queue setter.
     * @param queue Queue with package tgz key and owner
     */
    public void setPackages(final Queue<ProxyArtifactEvent> queue) {
        this.packages = queue;
    }

    /**
     * Repository storage setter.
     * @param storage Storage
     */
    public void setStorage(final Storage storage) {
        this.asto = storage;
    }
}
