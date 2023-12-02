/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.gem;

import com.artipie.asto.fs.FileStorage;
import com.artipie.gem.http.GemSlice;
import com.artipie.http.rs.RsStatus;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.WebClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A test for gem submit operation.
 *
 * @since 0.2
 */
@Disabled("Remove when #1317 will be done")
public class SubmitGemITCase {

    @Test
    public void submitResultsInOkResponse(@TempDir final Path temp) throws IOException {
        final Queue<ArtifactEvent> events = new LinkedList<>();
        final Vertx vertx = Vertx.vertx();
        final VertxSliceServer server = new VertxSliceServer(
            vertx,
            new GemSlice(new FileStorage(temp), Optional.of(events))
        );
        final WebClient web = WebClient.create(vertx);
        final int port = server.start();
        final byte[] gem = Files.readAllBytes(
            Paths.get("./src/test/resources/builder-3.2.4.gem")
        );
        final int code = web.post(port, "localhost", "/api/v1/gems")
            .rxSendBuffer(Buffer.buffer(gem))
            .blockingGet()
            .statusCode();
        MatcherAssert.assertThat(
            code,
            new IsEqual<>(Integer.parseInt(RsStatus.CREATED.code()))
        );
        MatcherAssert.assertThat("Upload event was added to queue", events.size() == 1);
        web.close();
        server.close();
        vertx.close();
    }
}
