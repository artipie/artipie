/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.npm.proxy.http;

import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rt.ByMethodsRule;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.http.slice.SliceSimple;
import com.artipie.npm.proxy.NpmProxy;
import com.artipie.scheduling.ProxyArtifactEvent;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import org.reactivestreams.Publisher;

/**
 * Main HTTP slice NPM Proxy adapter.
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (100 lines)
 */
public final class NpmProxySlice implements Slice {
    /**
     * Route.
     */
    private final SliceRoute route;

    /**
     * Ctor.
     *
     * @param path NPM proxy repo path ("" if NPM proxy should handle ROOT context path),
     *  or, in other words, repository name
     * @param npm NPM Proxy facade
     * @param packages Queue with uploaded from remote packages
     */
    @SuppressWarnings("PMD.ConstructorOnlyInitializesOrCallOtherConstructors")
    public NpmProxySlice(
        final String path, final NpmProxy npm, final Optional<Queue<ProxyArtifactEvent>> packages
    ) {
        final PackagePath ppath = new PackagePath(path);
        final AssetPath apath = new AssetPath(path);
        this.route = new SliceRoute(
            new RtRulePath(
                new RtRule.All(
                    new ByMethodsRule(RqMethod.GET),
                    new RtRule.ByPath(ppath.pattern())
                ),
                new LoggingSlice(
                    new DownloadPackageSlice(npm, ppath)
                )
            ),
            new RtRulePath(
                new RtRule.All(
                    new ByMethodsRule(RqMethod.GET),
                    new RtRule.ByPath(apath.pattern())
                ),
                new LoggingSlice(
                    new DownloadAssetSlice(npm, apath, packages, path)
                )
            ),
            new RtRulePath(
                RtRule.FALLBACK,
                new LoggingSlice(
                    new SliceSimple(
                        new RsNotFound()
                    )
                )
            )
        );
    }

    @Override
    public Response response(final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        return this.route.response(line, headers, body);
    }
}
