/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.pypi;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.ext.KeyLastPart;
import com.artipie.pypi.meta.Metadata;
import com.artipie.pypi.meta.PackageInfo;
import com.artipie.pypi.meta.ValidFilename;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.scheduling.ProxyArtifactEvent;
import com.artipie.scheduling.QuartzJob;
import com.jcabi.log.Logger;
import java.io.ByteArrayInputStream;
import java.util.Queue;
import org.quartz.JobExecutionContext;

/**
 * Job to process package, loaded via proxy and add corresponding info to
 * events queue.
 * @since 0.9
 */
public final class PyProxyPackageProcessor extends QuartzJob {

    /**
     * Repository type.
     */
    private static final String REPO_TYPE = "pypi-proxy";

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
    private BlockingStorage asto;

    @Override
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void execute(final JobExecutionContext context) {
        if (this.asto == null || this.packages == null || this.events == null) {
            super.stopJob(context);
        } else {
            //@checkstyle NestedIfDepthCheck (80 lines)
            while (!this.packages.isEmpty()) {
                final ProxyArtifactEvent event = this.packages.poll();
                if (event != null) {
                    final Key key = event.artifactKey();
                    final String filename = new KeyLastPart(key).get();
                    final byte[] archive = this.asto.value(key);
                    try {
                        final PackageInfo info = new Metadata.FromArchive(
                            new ByteArrayInputStream(archive), filename
                        ).read();
                        if (new ValidFilename(info, filename).valid()) {
                            this.events.add(
                                new ArtifactEvent(
                                    PyProxyPackageProcessor.REPO_TYPE, event.repoName(),
                                    "ANONYMOUS", String.join("/", info.name(), filename),
                                    info.version(), archive.length
                                )
                            );
                        } else {
                            Logger.error(
                                this,
                                String.format("Python proxy package %s is not valid", key.string())
                            );
                        }
                    // @checkstyle IllegalCatchCheck (1 line)
                    } catch (final Exception err) {
                        Logger.error(
                            this,
                            String.format(
                                "Failed to parse/check python proxy package %s: %s",
                                key.string(), err.getMessage()
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
        this.asto = new BlockingStorage(storage);
    }

}
