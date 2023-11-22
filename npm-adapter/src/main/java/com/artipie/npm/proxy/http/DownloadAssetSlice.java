/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.npm.proxy.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.Header;
import com.artipie.http.headers.Login;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import com.artipie.npm.misc.DateTimeNowStr;
import com.artipie.npm.proxy.NpmProxy;
import com.artipie.scheduling.ProxyArtifactEvent;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import org.reactivestreams.Publisher;

/**
 * HTTP slice for download asset requests.
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (200 lines)
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
    private final String rname;

    /**
     * Ctor.
     *
     * @param npm NPM Proxy facade
     * @param path Asset path helper
     * @param packages Queue with proxy packages and owner
     * @param rname Repository name
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    public DownloadAssetSlice(final NpmProxy npm, final AssetPath path,
        final Optional<Queue<ProxyArtifactEvent>> packages, final String rname) {
        this.npm = npm;
        this.path = path;
        this.packages = packages;
        this.rname = rname;
    }

    @Override
    public Response response(final String line,
        final Iterable<Map.Entry<String, String>> rqheaders,
        final Publisher<ByteBuffer> body) {
        final String tgz = this.path.value(new RequestLineFrom(line).uri().getPath());
        return new AsyncResponse(
            this.npm.getAsset(tgz).map(
                asset -> {
                    this.packages.ifPresent(
                        queue -> queue.add(
                            new ProxyArtifactEvent(
                                new Key.From(tgz), this.rname,
                                new Login(new Headers.From(rqheaders)).getValue()
                            )
                        )
                    );
                    return asset;
                })
                .map(
                    asset -> (Response) new RsFull(
                        RsStatus.OK,
                        new Headers.From(
                            new Header(
                                "Content-Type",
                                Optional.ofNullable(
                                    asset.meta().contentType()
                                ).orElseThrow(
                                    () -> new IllegalStateException(
                                        "Failed to get 'Content-Type'"
                                    )
                                )
                            ),
                            new Header(
                                "Last-Modified", Optional.ofNullable(
                                    asset.meta().lastModified()
                                ).orElse(new DateTimeNowStr().value())
                            )
                        ),
                        new Content.From(
                            asset.dataPublisher()
                        )
                    )
                )
                .toSingle(new RsNotFound())
                .to(SingleInterop.get())
        );
    }
}
