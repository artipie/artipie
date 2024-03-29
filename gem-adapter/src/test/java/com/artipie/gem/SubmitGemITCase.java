/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.gem;

import com.artipie.asto.fs.FileStorage;
import com.artipie.gem.http.GemSlice;
import com.artipie.http.RsStatus;
import com.artipie.scheduling.ArtifactEvent;
import com.artipie.security.policy.Policy;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.WebClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

/**
 * A test for gem submit operation.
 *
 * @since 0.2
 */
public class SubmitGemITCase {

    @Test
    public void submitResultsInOkResponse(@TempDir final Path temp) throws IOException {
        final Queue<ArtifactEvent> events = new LinkedList<>();
        final Vertx vertx = Vertx.vertx();
        try {
            try (VertxSliceServer server = new VertxSliceServer(
                vertx,
                new GemSlice(
                    new FileStorage(temp), Policy.FREE,
                    (username, password) -> Optional.empty(), "",
                    Optional.of(events)
                )
            )) {
                final WebClient web = WebClient.create(vertx);
                try {
                    final int port = server.start();
                    final byte[] gem = Files.readAllBytes(
                        Paths.get("./src/test/resources/builder-3.2.4.gem")
                    );
                    final int code = web.post(port, "localhost", "/api/v1/gems")
                        .rxSendBuffer(Buffer.buffer(gem))
                        .blockingGet()
                        .statusCode();
                    Assertions.assertEquals(RsStatus.CREATED.code(), code);
                    Assertions.assertEquals(1, events.size());
                } finally {
                    web.close();
                }
            }
        } finally {
            vertx.close();
        }
    }
}
