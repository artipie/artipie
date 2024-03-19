/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.adapters.file;

import com.artipie.asto.Content;
import com.artipie.asto.Storage;
import com.artipie.asto.cache.Cache;
import com.artipie.asto.cache.FromStorageCache;
import com.artipie.files.FileProxySlice;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.client.ClientSlices;
import com.artipie.http.client.auth.AuthClientSlice;
import com.artipie.http.rq.RequestLine;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.settings.repo.RepoConfig;

import java.util.Optional;
import java.util.Queue;

/**
 * File proxy slice created from config.
 */
public final class FileProxy implements Slice {

    private final Slice slice;

    /**
     * @param client HTTP client.
     * @param cfg Repository configuration.
     * @param events Artifact events queue
     */
    public FileProxy(
        ClientSlices client, RepoConfig cfg, Optional<Queue<ArtifactEvent>> events
    ) {
        final Optional<Storage> asto = cfg.storageOpt();
        this.slice = new FileProxySlice(
            AuthClientSlice.withUriClientSlice(client, cfg.remoteConfig()),
            asto.<Cache>map(FromStorageCache::new).orElse(Cache.NOP),
            asto.flatMap(ignored -> events),
            cfg.name()
        );
    }

    @Override
    public Response response(
        RequestLine line,
        Headers headers,
        Content body
    ) {
        return slice.response(line, headers, body);
    }
}
