/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.scheduling;

import com.artipie.asto.Key;
import com.artipie.http.Headers;
import com.artipie.http.headers.Login;
import java.util.Map;
import java.util.Queue;

/**
 * Repository events.
 * @since 1.3
 */
public final class RepositoryEvents {

    /**
     * Unknown version.
     */
    private static final String VERSION = "UNKNOWN";

    /**
     * Repository type.
     */
    private final String rtype;

    /**
     * Repository name.
     */
    private final String rname;

    /**
     * Artifact events queue.
     */
    private final Queue<ArtifactEvent> queue;

    /**
     * Ctor.
     * @param rtype Repository type
     * @param rname Repository name
     * @param queue Artifact events queue
     */
    public RepositoryEvents(
        final String rtype, final String rname, final Queue<ArtifactEvent> queue
    ) {
        this.rtype = rtype;
        this.rname = rname;
        this.queue = queue;
    }

    /**
     * Adds event to queue, artifact name is the key and version is "UNKNOWN",
     * owner is obtained from headers.
     * @param key Artifact key
     * @param size Artifact size
     * @param headers Request headers
     */
    public void addUploadEventByKey(final Key key, final long size,
        final Iterable<Map.Entry<String, String>> headers) {
        this.queue.add(
            new ArtifactEvent(
                this.rtype, this.rname, new Login(new Headers.From(headers)).getValue(),
                key.string(), RepositoryEvents.VERSION, size
            )
        );
    }

    /**
     * Adds event to queue, artifact name is the key and version is "UNKNOWN",
     * owner is obtained from headers.
     * @param key Artifact key
     */
    public void addDeleteEventByKey(final Key key) {
        this.queue.add(
            new ArtifactEvent(this.rtype, this.rname, key.string(), RepositoryEvents.VERSION)
        );
    }
}
