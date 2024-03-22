/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.files;

import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.BaseResponse;
import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.auth.AuthUser;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthzSlice;
import com.artipie.http.auth.OperationControl;
import com.artipie.http.headers.Accept;
import com.artipie.http.headers.ContentType;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rt.ByMethodsRule;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.HeadSlice;
import com.artipie.http.slice.KeyFromPath;
import com.artipie.http.slice.SliceDelete;
import com.artipie.http.slice.SliceDownload;
import com.artipie.http.slice.SliceSimple;
import com.artipie.http.slice.SliceUpload;
import com.artipie.http.slice.SliceWithHeaders;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.scheduling.RepositoryEvents;
import com.artipie.security.perms.Action;
import com.artipie.security.perms.AdapterBasicPermission;
import com.artipie.security.policy.Policy;
import com.artipie.vertx.VertxSliceServer;

import java.util.Optional;
import java.util.Queue;
import java.util.regex.Pattern;

/**
 * A {@link Slice} which servers binary files.
 */
public final class FilesSlice extends Slice.Wrap {

    /**
     * HTML mime type.
     */
    public static final String HTML_TEXT = "text/html";

    /**
     * Plain text mime type.
     */
    public static final String PLAIN_TEXT = "text/plain";

    /**
     * Repo name for the test cases when policy and permissions are not used
     * and any actions are allowed for anyone.
     */
    static final String ANY_REPO = "*";

    /**
     * Mime type of file.
     */
    private static final String OCTET_STREAM = "application/octet-stream";

    /**
     * JavaScript Object Notation mime type.
     */
    private static final String JSON = "application/json";

    /**
     * Repository type.
     */
    private static final String REPO_TYPE = "file";

    /**
     * Ctor used by Artipie server which knows `Authentication` implementation.
     * @param storage The storage. And default parameters for free access.
     * @param perms Access permissions.
     * @param auth Auth details.
     * @param name Repository name
     * @param events Repository artifact events
     */
    public FilesSlice(
        final Storage storage, final Policy<?> perms, final Authentication auth, final String name,
        final Optional<Queue<ArtifactEvent>> events
    ) {
        super(
            new SliceRoute(
                new RtRulePath(
                    new ByMethodsRule(RqMethod.HEAD),
                    new BasicAuthzSlice(
                        new SliceWithHeaders(
                            new FileMetaSlice(new HeadSlice(storage), storage),
                            Headers.from(ContentType.mime(FilesSlice.OCTET_STREAM))
                        ),
                        auth,
                        new OperationControl(
                            perms, new AdapterBasicPermission(name, Action.Standard.READ)
                        )
                    )
                ),
                new RtRulePath(
                    ByMethodsRule.Standard.GET,
                    new BasicAuthzSlice(
                        new SliceRoute(
                            new RtRulePath(
                                new RtRule.ByHeader(
                                    Accept.NAME,
                                    Pattern.compile(FilesSlice.PLAIN_TEXT)
                                ),
                                new ListBlobsSlice(
                                    storage,
                                    BlobListFormat.Standard.TEXT,
                                    FilesSlice.PLAIN_TEXT
                                )
                            ),
                            new RtRulePath(
                                new RtRule.ByHeader(
                                    Accept.NAME,
                                    Pattern.compile(FilesSlice.JSON)
                                ),
                                new ListBlobsSlice(
                                    storage,
                                    BlobListFormat.Standard.JSON,
                                    FilesSlice.JSON
                                )
                            ),
                            new RtRulePath(
                                new RtRule.ByHeader(
                                    Accept.NAME,
                                    Pattern.compile(FilesSlice.HTML_TEXT)
                                ),
                                new ListBlobsSlice(
                                    storage,
                                    BlobListFormat.Standard.HTML,
                                    FilesSlice.HTML_TEXT
                                )
                            ),
                            new RtRulePath(
                                RtRule.FALLBACK,
                                new SliceWithHeaders(
                                    new FileMetaSlice(
                                        new SliceDownload(storage),
                                        storage
                                    ),
                                    Headers.from(ContentType.mime(FilesSlice.OCTET_STREAM))
                                )
                            )
                        ),
                        auth,
                        new OperationControl(
                            perms, new AdapterBasicPermission(name, Action.Standard.READ)
                        )
                    )
                ),
                new RtRulePath(
                    ByMethodsRule.Standard.PUT,
                    new BasicAuthzSlice(
                        new SliceUpload(
                            storage,
                            KeyFromPath::new,
                            events.map(
                                queue -> new RepositoryEvents(FilesSlice.REPO_TYPE, name, queue)
                            )
                        ),
                        auth,
                        new OperationControl(
                            perms, new AdapterBasicPermission(name, Action.Standard.WRITE)
                        )
                    )
                ),
                new RtRulePath(
                    ByMethodsRule.Standard.DELETE,
                    new BasicAuthzSlice(
                        new SliceDelete(
                            storage,
                            events.map(
                                queue -> new RepositoryEvents(FilesSlice.REPO_TYPE, name, queue)
                            )
                        ),
                        auth,
                        new OperationControl(
                            perms, new AdapterBasicPermission(name, Action.Standard.DELETE)
                        )
                    )
                ),
                new RtRulePath(
                    RtRule.FALLBACK,
                    new SliceSimple(BaseResponse.methodNotAllowed())
                )
            )
        );
    }

    /**
     * Entry point.
     * @param args Command line args
     */
    public static void main(final String... args) {
        final int port = 8080;
        try (
            VertxSliceServer server = new VertxSliceServer(
                new FilesSlice(
                    new InMemoryStorage(), Policy.FREE,
                    (username, password) -> Optional.of(AuthUser.ANONYMOUS),
                    FilesSlice.ANY_REPO, Optional.empty()), port
            )
        ) {
            server.start();
        }
    }
}
