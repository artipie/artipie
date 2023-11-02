/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.npm.events;

import com.artipie.asto.Key;
import com.artipie.asto.Meta;
import com.artipie.asto.Storage;
import com.artipie.asto.streams.ContentAsStream;
import com.artipie.npm.Publish;
import com.artipie.npm.TgzArchive;
import com.artipie.npm.http.UploadSlice;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.scheduling.ProxyArtifactEvent;
import com.artipie.scheduling.QuartzJob;
import com.jcabi.log.Logger;
import java.util.Optional;
import java.util.Queue;
import javax.json.Json;
import javax.json.JsonObject;
import org.quartz.JobExecutionContext;

/**
 * We can assume that repository actually contains some package, if:
 * <br/>
 * 1) tgz archive is valid and we obtained package id and version from it<br/>
 * 2) repository has corresponding package json metadata file with such version and
 *   path to tgz
 * <br/>
 * When both conditions a met, we can add package record into database.
 * @since 1.5
 */
@SuppressWarnings("PMD.DataClass")
public final class NpmProxyPackageProcessor extends QuartzJob {

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

    /**
     * Artipie host (host only).
     */
    private String host;

    @Override
    @SuppressWarnings("PMD.CyclomaticComplexity")
    public void execute(final JobExecutionContext context) {
        // @checkstyle BooleanExpressionComplexityCheck (6 lines)
        // @checkstyle NestedIfDepthCheck (20 lines)
        if (this.asto == null || this.packages == null || this.host == null
            || this.events == null) {
            super.stopJob(context);
        } else {
            while (!this.packages.isEmpty()) {
                final ProxyArtifactEvent item = this.packages.poll();
                if (item != null) {
                    final Optional<Publish.PackageInfo> info = this.info(item.artifactKey());
                    if (info.isPresent() && this.checkMetadata(info.get(), item)) {
                        this.events.add(
                            new ArtifactEvent(
                                UploadSlice.REPO_TYPE, item.repoName(), item.ownerLogin(),
                                info.get().packageName(), info.get().packageVersion(),
                                info.get().tarSize()
                            )
                        );
                    } else {
                        Logger.info(
                            this,
                            String.format("Package %s is not valid", item.artifactKey().string())
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

    /**
     * Set repository host.
     * @param url The host
     */
    public void setHost(final String url) {
        this.host = url;
        if (this.host.endsWith("/")) {
            this.host = this.host.substring(0, this.host.length() - 2);
        }
    }

    /**
     * Method checks that package metadata contains version from package info and
     * path in `dist` fiend to corresponding tgz package.
     * @param info Info from tgz to check
     * @param item Item with tgz file key path in storage
     * @return True, if package meta.jaon metadata contains the version and path
     */
    private boolean checkMetadata(final Publish.PackageInfo info, final ProxyArtifactEvent item) {
        final Key key = new Key.From(info.packageName(), "meta.json");
        return this.asto.value(key)
            .thenCompose(
                content -> new ContentAsStream<>(content).process(
                    input -> Json.createReader(input).readObject()
                )
            ).thenApply(
                json -> {
                    final JsonObject version = ((JsonObject) json).getJsonObject("versions")
                        .getJsonObject(info.packageVersion());
                    boolean res = false;
                    if (version != null) {
                        final JsonObject dist = version.getJsonObject("dist");
                        if (dist != null) {
                            final String tarball = dist.getString("tarball");
                            res = tarball.equals(String.format("/%s", item.artifactKey().string()))
                                || tarball.contains(
                                    String.join(
                                        "/", this.host, item.repoName(), item.artifactKey().string()
                                    )
                                );
                        }
                    }
                    return res;
                }
            ).handle(
                (correct, error) -> {
                    final boolean res;
                    if (error == null) {
                        res = correct;
                    } else {
                        Logger.error(
                            this,
                            String.format(
                                "Error while checking %s for dist %s \n%s",
                                key.string(), item.artifactKey().string(), error.getMessage()
                            )
                        );
                        res = false;
                    }
                    return res;
                }
            ).join();
    }

    /**
     * Read package info, canonical name, version and calc package size for tgz.
     * @param tgz Tgz storage key
     * @return Package info
     */
    private Optional<Publish.PackageInfo> info(final Key tgz) {
        return this.asto.value(tgz).thenCompose(
            content -> new ContentAsStream<>(content).<JsonObject>process(
                input -> new TgzArchive.JsonFromStream(input).json()
            )
        ).thenCombine(
            this.asto.metadata(tgz).<Long>thenApply(meta -> meta.read(Meta.OP_SIZE).get()),
            (json, size) -> new Publish.PackageInfo(
                ((JsonObject) json).getString("name"),
                ((JsonObject) json).getString("version"), size
            )
        ).handle(
            (info, error) -> {
                final Optional<Publish.PackageInfo> res;
                if (error == null) {
                    res = Optional.of(info);
                } else {
                    Logger.error(
                        this,
                        String.format(
                            "Error while reading tgz %s info\n%s", tgz.string(), error.getMessage()
                        )
                    );
                    res = Optional.empty();
                }
                return res;
            }
        ).join();
    }

}
