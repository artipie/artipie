/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm.proxy.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.ResponseBuilder;
import com.artipie.http.Slice;
import com.artipie.http.headers.ContentType;
import com.artipie.http.headers.Login;
import com.artipie.http.rq.RequestLine;
import com.artipie.npm.misc.DateTimeNowStr;
import com.artipie.npm.proxy.NpmProxy;
import com.artipie.scheduling.ProxyArtifactEvent;
import com.google.common.base.Strings;
import hu.akarnokd.rxjava2.interop.SingleInterop;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP slice for download asset requests.
 */
public final class DownloadAssetSlice implements Slice {
    /**
     * NPM Proxy facade.
     */
    private final NpmProxy npm;

    /**
     * Asset path helper.
     */
    private final AssetPath path;

    /**
     * Queue with packages and owner names.
     */
    private final Optional<Queue<ProxyArtifactEvent>> packages;

    /**
     * Repository name.
     */
    private final String repoName;

    /**
     * @param npm NPM Proxy facade
     * @param path Asset path helper
     * @param packages Queue with proxy packages and owner
     * @param repoName Repository name
     */
    public DownloadAssetSlice(final NpmProxy npm, final AssetPath path,
        final Optional<Queue<ProxyArtifactEvent>> packages, final String repoName) {
        this.npm = npm;
        this.path = path;
        this.packages = packages;
        this.repoName = repoName;
    }

    @Override
    public CompletableFuture<Response> response(final RequestLine line,
                                                final Headers rqheaders,
                                                final Content body) {
        final String tgz = this.path.value(line.uri().getPath());
        return this.npm.getAsset(tgz).map(
                asset -> {
                    this.packages.ifPresent(
                        queue -> queue.add(
                            new ProxyArtifactEvent(
                                new Key.From(tgz), this.repoName,
                                new Login(rqheaders).getValue()
                            )
                        )
                    );
                    return asset;
                })
            .map(
                asset -> {
                    String mime = asset.meta().contentType();
                    if (Strings.isNullOrEmpty(mime)){
                        throw new IllegalStateException("Failed to get 'Content-Type'");
                    }
                    String lastModified = asset.meta().lastModified();
                    if(Strings.isNullOrEmpty(lastModified)){
                        lastModified = new DateTimeNowStr().value();
                    }
                    return ResponseBuilder.ok()
                        .header(ContentType.mime(mime))
                        .header("Last-Modified", lastModified)
                        .body(asset.dataPublisher())
                        .build();
                }
            )
            .toSingle(ResponseBuilder.notFound().build())
            .to(SingleInterop.get())
            .toCompletableFuture();
    }
}
