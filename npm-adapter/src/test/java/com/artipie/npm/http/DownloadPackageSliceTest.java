/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */

package com.artipie.npm.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.slice.TrimPathSlice;
import com.artipie.npm.RandomFreePort;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.WebClient;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Tests Download Package Slice works.
 * @since 0.6
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
public class DownloadPackageSliceTest {
    @Test
    public void downloadMetaWorks() throws IOException, ExecutionException, InterruptedException {
        final Vertx vertx = Vertx.vertx();
        final Storage storage = new InMemoryStorage();
        storage.save(
            new Key.From("@hello", "simple-npm-project", "meta.json"),
            new Content.From(
                IOUtils.resourceToByteArray("/storage/@hello/simple-npm-project/meta.json")
            )
        ).get();
        final int port = new RandomFreePort().value();
        final VertxSliceServer server = new VertxSliceServer(
            vertx,
            new TrimPathSlice(
                new DownloadPackageSlice(
                    URI.create(String.format("http://127.0.0.1:%d/ctx", port)).toURL(),
                    storage
                ),
                "ctx"
            ),
            port
        );
        server.start();
        final String url = String.format(
            "http://127.0.0.1:%d/ctx/@hello/simple-npm-project", port
        );
        final WebClient client = WebClient.create(vertx);
        final JsonObject json = client.getAbs(url).rxSend().blockingGet().body().toJsonObject();
        MatcherAssert.assertThat(
            json.getJsonObject("versions").getJsonObject("1.0.1")
                .getJsonObject("dist").getString("tarball"),
            new IsEqual<>(
                String.format(
                    "%s/-/@hello/simple-npm-project-1.0.1.tgz",
                    url
                )
            )
        );
        server.stop();
        vertx.close();
    }
}
